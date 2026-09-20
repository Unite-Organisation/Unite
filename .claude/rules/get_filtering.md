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
                .buildingId(Filter.of(scope.buildingId()))
                .postType(Filter.of(postType))
                .visibleFrom(visibleFromFilter)
                .visibleTo(visibleToFilter)
                .build();
    }
}
```

- Wrap plain values with `Filter.of(...)`, comparison values with `ComparisonFilter.of(value, modifier)`.
  Both are `Filter<T>`, so the service decides whether a field compares by equality or by range -
  the filter itself does not care.
- Apply defaults here when a missing param should still narrow the query - e.g. posts default to
  "visible now" instead of returning everything.
- Inject `Clock` for time-based defaults, never call `LocalDateTime.now()` directly, so it stays testable.
- The service always returns a fully built filter, never `null`.

### 3. Filter - jOOQ conditions

One `<Entity>Filter` in `com.app.prod.utils.filters`, `@Builder`, implementing `PredicateFilter`.
Every field is a `Filter<T>` - never a raw value, never an `Optional`. A `Filter` knows the value and
how it turns into a condition; `ComparisonFilter<T> extends Filter<T>` swaps equality for a range, so
one `match(...)` covers both and the filter never spells out which fields are range params.

`combineConditions()` is a declarative list built with `Criteria` - one field, one line. Never
hand-roll an `ArrayList` and `ifPresent(... ::add)`.

```java
@Builder
public class PostFilter implements PredicateFilter {
    Filter<UUID> buildingId;   // from the authorized BuildingScope, must be present
    Filter<UUID> createdBy;
    Filter<PostType> postType;
    Filter<LocalDateTime> visibleFrom;
    Filter<LocalDateTime> visibleTo;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                required(POST.BUILDING_ID, buildingId),
                match(POST.CREATED_BY, createdBy),
                matchEnum(POST.POST_TYPE, postType),
                match(POST.VISIBLE_FROM, visibleFrom),
                match(POST.VISIBLE_TO, visibleTo)
        );
    }
}
```

| factory                     | use for                                                             |
|-----------------------------|---------------------------------------------------------------------|
| `match(field, filter)`      | any optional field - equality or range, the `Filter` decides         |
| `matchEnum(field, filter)`  | an enum stored as its `name()`                                       |
| `required(field, filter)`   | a field the query must not run without, above all `scope.buildingId()` |
| `always(condition)`         | a condition with no request param behind it                          |
| `when(filter, mapper)`      | anything else - a mapped enum, or a `Related` cross-table condition   |

- A criterion that is empty adds no condition, so an empty filter yields an empty list and
  `parseFilter()` reduces that to `DSL.trueCondition()`.
- `required` is the exception: with no value it throws `IllegalApplicationStateException`
  (`Code.MANDATORY_FILTER_MISSING`) instead of quietly dropping the condition. A `buildingId` that
  silently disappears would return every building's rows, so this fails the request instead.
- A field left unset on the `@Builder` is `null`, and `Criteria` reads that as empty - so a filter
  can gain a field without touching the places that build it.
- Use the generated jOOQ table constants (`org.jooq.sources.Tables`), never raw SQL strings.
- `matchEnum` exists instead of a `match` overload because `Filter<E>` and `Filter<T>` clash after erasure.

### 3a. Conditions reaching another table

A filter never adds a join. A join onto a 1:n relation multiplies rows and breaks the
`offset/limit` paging every filtered endpoint relies on, and it would make the repository query
depend on which filters happen to be active. Express it as a correlated subquery through `Related`
- Postgres plans an `EXISTS` as a semi-join, so nothing is lost.

```java
// 1:n or n:m - "the caller attends this event", see PostFilter#attendedBy
when(attendedBy, userId -> InteractionFields.reactedBy(POST.ID, InteractionEntityType.POST, ATTENDING, userId))

// n:1 - a value from the table next door, usable like any other field
eqEnum(lookup(USER_ROLE.USER_ROLE_, USER_ROLE, USER_ROLE.ID.eq(APP_USER.USER_ROLE)), role)
```

- `Related.existsIn` / `notExistsIn` for 1:n and n:m, `Related.lookup` for n:1.
- Never a join for this: `attendedBy` implemented as `join user_interaction` would return an event
  with three attendees three times, so `limit 20` would yield fewer than 20 posts.
- When the subquery is non-trivial or shared by several filters, it becomes a static method in
  `<Feature>Fields` in the `repository` package of the feature **owning that table** - see
  `EmailDeliveryFields.lastDeliveryStatus(...)`, used by `BuildingUserFilter`. The filter stays a
  one-liner and the subquery is testable on its own.
- The only case a real join is justified is when the joined table must appear in `SELECT` or
  `ORDER BY`. That is the repository's call, not the filter's: the repository declares the join
  statically and the filter still only contributes conditions.

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

`PostFilter`, `FacilityFilter`, `FacilityReservationFilter`, `PollFilter`, `OfferingFilter`,
`RequestFilter`, `InteractionFilter`, `BuildingUserFilter` - all in `com.app.prod.utils.filters`,
next to the shared `Filter`, `ComparisonFilter`, `Criteria`, `Criterion` and `Related`.

Follow whichever is closest when adding a new one. A new kind of condition belongs in `Criteria`
(or `Related`, when it reaches another table) rather than being duplicated per filter; a new way of
comparing a single value belongs in a `Filter` subclass, the way `ComparisonFilter` does it.
