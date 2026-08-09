## Filtering GET endpoints

Every GET endpoint returning a collection filters through a `PredicateFilter`, the way posts do.
Reference implementation: `PostRestApi#getPosts` -> `PostFilteringService` -> `PostFilter` -> `PostRepository#findForUser`.

Never filter in Java after fetching, and never build jOOQ `Condition` objects inside a controller
or a service method - conditions belong in the filter, the repository only applies them.

The flow is always the same:

```
<Entity>FilterRequest  ->  <Entity>FilteringService.prepareFilter(scope, request)  ->  <Entity>Filter  ->  .where(filter.parseFilter())
```

### 1. Controller - request params

Request params arrive as a single `@ModelAttribute` object, one `<Entity>FilterRequest` per
feature, extending `FilterRequest` (`com.app.prod.utils.filters`). The controller does not
interpret it - it passes it straight to the filtering service and hands the resulting filter to
the domain service.

```java
@GetMapping()
public List<PostResponse> getPosts(BuildingScope scope, @Valid @ModelAttribute PostFilterRequest request){
    PostFilter filter = postFilteringService.prepareFilter(scope, request);
    return postService.getPosts(request.pagination(), scope, filter);
}
```

```java
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PostFilterRequest extends FilterRequest {
    private PostType postType;
    private UUID createdBy;
    @DateTimeFormat(iso = DATE_TIME)
    private LocalDateTime visibleFrom;
    private ComparisonFilter.Modifier visibleFromModifier;
    ...
}
```

- Fields live in the request object, never as flat `@RequestParam` - Spring binds each query
  param onto the matching field, an absent one stays `null` and means "do not narrow the result".
- Every GET endpoint accepts pagination, because `page` and `pageSize` come from the
  `FilterRequest` base class. Both are optional: `request.pagination()` falls back to
  `Pagination.DEFAULT_PAGE` / `Pagination.DEFAULT_PAGE_SIZE`, so an endpoint the frontend does not
  paginate simply gets the default page. `@Valid` still rejects explicit out-of-range values.
- Paging stays separate from filtering - it is read off the request object, never a field of the
  `<Entity>Filter`.
- Range-style params come in pairs: the value plus a `ComparisonFilter.Modifier`
  (`LESS_OR_EQUAL_THAN`, `GREATER_OR_EQUAL_THAN`, `EQUAL`), named `<field>Modifier`.
- Dates use `@DateTimeFormat(iso = DATE_TIME)` on the field.
- `@SuperBuilder` + `@NoArgsConstructor` - the no-arg constructor and setters are what Spring binds
  through, the builder is what tests construct the request with.
- Access scoping is not a request field - the building comes from the authorized
  `BuildingScope` (see `building_scope.md`), which the controller declares as a parameter and hands
  to the filtering service. Migrated features carry it as a mandatory `buildingId` filter field.
  A `<Entity>FilterRequest` never carries `buildingId`.

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

`PostFilter`, `FacilityFilter`, `PollFilter`, `OfferingFilter`, `RequestFilter` - all in `com.app.prod.utils.filters`.
Follow whichever is closest when adding a new one, and add a shared field to `ComparisonFilter`
rather than duplicating comparison logic per filter.
