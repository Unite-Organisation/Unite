## Building scope

The building is the unit every business operation is performed in. A resident belongs to exactly
one building, a manager picks the building they are working in after logging in. The frontend sends
that choice as a `buildingId` request parameter, the backend authorizes it on every request.

Reference implementation: `PostRestApi` -> `BuildingScopeArgumentResolver` -> `BuildingAccessService`
-> `BuildingAccessStrategy`. Migrated features: `post`, `interaction`.

Never resolve "which buildings can this user see" inside a query, and never take `buildingId` from a
request body - both were how the old area based visibility leaked access rules into every repository.

### 1. Controller - declare the scope

```java
@GetMapping()
public List<PostResponse> getPosts(
        BuildingScope scope,
        @Valid @ModelAttribute Pagination pagination,
        @RequestParam(required = false) PostType postType
){
    PostFilter filter = postFilteringService.prepareFilter(scope, postType);
    return postService.getPosts(pagination, scope, filter);
}
```

- A `BuildingScope` parameter is bound by `BuildingScopeArgumentResolver` from the **required**
  `buildingId` query parameter, and the request is rejected with 403 when the caller has no access.
  A handler declaring it is therefore always authorized - there is no guard call to forget.
- `buildingId` is a query parameter on writes too (`POST /post/announcement?buildingId=...`), because
  the resolver cannot read the request body.
- Request DTOs never carry `buildingId` or `areaId` - two sources of truth is how access checks and
  writes end up disagreeing.
- Endpoints that are not building scoped simply do not declare the parameter: `/auth/**`, the
  building picker itself (`GET /building`), user profile, healthcheck.
- `BuildingScopedEndpointsIT` fails the build when a controller in a migrated package misses the
  parameter. Add the package to that test when migrating the next feature.

### 2. Access rules - one strategy per role

`BuildingAccessService` is the single place answering "may this caller act in this building". It
routes on the role from the token to a `BuildingAccessStrategy`, but the answer always comes from
the database:

| Role       | Rule                                                     |
|------------|----------------------------------------------------------|
| `RESIDENT` | `app_user.building_id` on the caller's own row (no query) |
| `MANAGER`  | a `building_manager` row for (user, building)             |
| `ADMIN`    | the building exists                                       |

- A building the caller may not enter is rejected exactly like one that does not exist
  (`UnauthorizedDataAccessException` + `Code.BUILDING_ACCESS_DENIED`), so the API never confirms
  which buildings are out there.
- A role without a strategy cannot enter any scoped endpoint.
- `buildingId` is never read from the JWT: a manager can lose a building while their token is alive.

### 3. BuildingScope is a proof of authorization

`BuildingScope` has a package private constructor, so only `BuildingAccessService` can create one.
A service or repository taking a `BuildingScope` can rely on the check having happened - that is the
whole point of passing it around instead of a bare `UUID`.

Tests that call a service directly build one through `com.app.prod.access.TestBuildingScope`, which
lives in the same package under `src/test` on purpose.

### 4. Filters and repositories

The building is a plain equality condition once access is settled, so it belongs in the filter:

```java
return PostFilter.builder()
        .buildingId(scope.buildingId())   // mandatory field, never Optional
        .postType(Optional.ofNullable(postType))
        .build();
```

- The `<Entity>FilteringService.prepareFilter(...)` takes `BuildingScope` as its first argument, so a
  filter without a building cannot be built.
- The repository keeps its single `.where(filter.parseFilter())` - no visibility subqueries, no joins
  onto "buildings visible to user".
- A user id passed to a repository is only for personalisation (e.g. "did I react to this post"),
  never for access control. Name such parameters `viewerId`.
