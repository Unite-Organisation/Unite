## Filtering GET endpoints

Every GET endpoint returning a collection filters through a `PredicateFilter`, the way posts do.
Reference implementation: `PostRestApi#getPosts` -> `PostFilteringService` -> `PostFilter` -> `PostRepository#findForUser`.

Never filter in Java after fetching, and never build jOOQ `Condition` objects inside a controller
or a service method - conditions belong in the filter, the repository only applies them.

The flow is always the same:

```
@RequestParam  ->  <Entity>FilteringService.prepareFilter(...)  ->  <Entity>Filter  ->  .where(filter.parseFilter())
```

### 1. Controller - request params

Filter values arrive as flat, optional `@RequestParam`. The controller does not interpret them -
it passes them straight to the filtering service and hands the resulting filter to the domain service.

```java
@GetMapping()
public List<PostResponse> getPosts(
        BuildingScope scope,
        @Valid @ModelAttribute Pagination pagination,
        @RequestParam(required = false) PostType postType,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) LocalDateTime visibleFrom,
        @RequestParam(required = false) ComparisonFilter.Modifier visibleFromModifier,
        @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) LocalDateTime visibleTo,
        @RequestParam(required = false) ComparisonFilter.Modifier visibleToModifier
){
    PostFilter filter = postFilteringService.prepareFilter(scope, postType, visibleFrom, visibleFromModifier, visibleTo, visibleToModifier);
    return postService.getPosts(pagination, scope, filter);
}
```

- All filter params are `required = false` - an absent param means "do not narrow the result".
- Range-style params come in pairs: the value plus a `ComparisonFilter.Modifier`
  (`LESS_OR_EQUAL_THAN`, `GREATER_OR_EQUAL_THAN`, `EQUAL`), named `<field>Modifier`.
- Dates use `@DateTimeFormat(iso = DATE_TIME)`.
- Paging is separate from filtering - `@Valid @ModelAttribute Pagination`, never a field of the filter.
- Access scoping is not a filter param either - the building comes from the authorized
  `BuildingScope` (see `building_scope.md`), which the controller declares as a parameter and hands
  to the filtering service. Migrated features carry it as a mandatory `buildingId` filter field.

### 2. FilteringService - params to filter

One `<Entity>FilteringService` per feature, a `@Component` in the feature's `service` package.
This is the only place where defaults and business rules about filtering live.

```java
@Component
@RequiredArgsConstructor
public class PostFilteringService {

    private final Clock clock;

    public PostFilter prepareFilter(
            BuildingScope scope,
            PostType postType,
            LocalDateTime visibleFrom, ComparisonFilter.Modifier visibleFromModifier,
            LocalDateTime visibleTo, ComparisonFilter.Modifier visibleToModifier
    ) {
        ComparisonFilter<LocalDateTime> visibleToFilter = ComparisonFilter.of(visibleTo, visibleToModifier);
        if (visibleToFilter.isEmpty()) {
            visibleToFilter = ComparisonFilter.of(LocalDateTime.now(clock), ComparisonFilter.Modifier.GREATER_OR_EQUAL_THAN);
        }
        ...
        return PostFilter.builder()
                .buildingId(scope.buildingId())
                .postType(Optional.ofNullable(postType))
                .visibleFrom(visibleFromFilter)
                .visibleTo(visibleToFilter)
                .build();
    }
}
```

- Wrap plain values with `Optional.ofNullable(...)`, comparison values with `ComparisonFilter.of(value, modifier)`.
- Apply defaults here when a missing param should still narrow the query - e.g. posts default to
  "visible now" instead of returning everything.
- Inject `Clock` for time-based defaults, never call `LocalDateTime.now()` directly, so it stays testable.
- The service always returns a fully built filter, never `null`.

### 3. Filter - jOOQ conditions

One `<Entity>Filter` in `com.app.prod.utils.filters`, `@Builder`, implementing `PredicateFilter`.
Fields are `Optional<T>` or `ComparisonFilter<T>` - never raw nullable values.

```java
@Builder
public class PostFilter implements PredicateFilter {
    UUID buildingId;   // from the authorized BuildingScope, always present
    Optional<PostType> postType;
    ComparisonFilter<LocalDateTime> visibleFrom;
    ComparisonFilter<LocalDateTime> visibleTo;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        conditionList.add(POST.BUILDING_ID.eq(buildingId));
        postType.ifPresent(r -> conditionList.add(POST.POST_TYPE.eq(r.name())));
        visibleFrom.toCondition(POST.VISIBLE_FROM).ifPresent(conditionList::add);
        visibleTo.toCondition(POST.VISIBLE_TO).ifPresent(conditionList::add);

        return conditionList;
    }
}
```

- `combineConditions()` only adds a condition when the value is present - an empty filter yields an
  empty list, and `parseFilter()` reduces that to `DSL.trueCondition()`.
- Use the generated jOOQ table constants (`org.jooq.sources.Tables`), never raw SQL strings.
- Enums are compared through `.name()`, matching how they are stored.

### 4. Repository - applying the filter

The jOOQ repository takes the filter as a parameter and applies it with a single `.where(filter.parseFilter())`.

```java
public List<PostResponse> findPosts(UUID viewerId, Pagination pagination, PostFilter filter) {
    return dslContext.select(...)
            .from(POST)
            .where(filter.parseFilter())
            .orderBy(POST.CREATED_AT)
            .offset(pagination.getOffset())
            .limit(pagination.pageSize())
            .fetch(record -> new PostResponse(...));
}
```

- Exactly one `.where(filter.parseFilter())` - additional `.and(...)` calls in the query mean a
  condition that belongs in the filter leaked into the repository.
- No access-control joins or visibility subqueries in the repository - access is already settled by
  the `BuildingScope` the filter was built from. A `viewerId` parameter is only for personalisation.
- Always paginate with `.offset(pagination.getOffset()).limit(pagination.pageSize())` and keep a
  deterministic `.orderBy(...)`.
- Repositories extend `BaseJooqRepository<Table, Record, Id>`.

### Existing filters

`PostFilter`, `PollFilter`, `OfferingFilter`, `RequestFilter` - all in `com.app.prod.utils.filters`.
Follow whichever is closest when adding a new one, and add a shared field to `ComparisonFilter`
rather than duplicating comparison logic per filter.
