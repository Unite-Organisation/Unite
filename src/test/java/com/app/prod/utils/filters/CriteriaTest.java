package com.app.prod.utils.filters;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.AppException;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import com.app.prod.post.enums.PostType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.always;
import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.matchEnum;
import static com.app.prod.utils.filters.Criteria.required;
import static com.app.prod.utils.filters.Criteria.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;
import static org.jooq.sources.Tables.POLL;
import static org.jooq.sources.Tables.POLL_OPTION;
import static org.jooq.sources.Tables.POST;

class CriteriaTest {

    private static final UUID BUILDING_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 2, 12, 0);

    @Test
    void shouldCompareByEqualityForAPlainFilter() {
        assertThat(match(POST.CREATED_BY, Filter.of(BUILDING_ID)).toCondition())
                .hasValueSatisfying(c -> assertThat(c).hasToString(POST.CREATED_BY.eq(BUILDING_ID).toString()));
    }

    @Test
    void shouldLetTheFilterDecideHowItCompares() {
        var criterion = match(POST.VISIBLE_FROM, ComparisonFilter.of(NOW, ComparisonFilter.Modifier.LESS_OR_EQUAL_THAN));

        assertThat(criterion.toCondition())
                .hasValueSatisfying(c -> assertThat(c).hasToString(POST.VISIBLE_FROM.le(NOW).toString()));
    }

    @Test
    void shouldProduceNoConditionForAbsentValue() {
        assertThat(match(POST.CREATED_BY, Filter.<UUID>empty()).toCondition()).isEmpty();
        assertThat(matchEnum(POST.POST_TYPE, Filter.<PostType>empty()).toCondition()).isEmpty();
        assertThat(match(POST.VISIBLE_FROM, ComparisonFilter.<LocalDateTime>empty()).toCondition()).isEmpty();
        assertThat(when(Filter.<PostType>empty(), type -> POST.POST_TYPE.eq(type.name())).toCondition()).isEmpty();
    }

    @Test
    void shouldTreatFieldLeftUnsetOnBuilderAsAbsent() {
        assertThat(match(POST.CREATED_BY, (Filter<UUID>) null).toCondition()).isEmpty();
        assertThat(matchEnum(POST.POST_TYPE, (Filter<PostType>) null).toCondition()).isEmpty();
        assertThat(when((Filter<PostType>) null, type -> POST.POST_TYPE.eq(type.name())).toCondition()).isEmpty();
    }

    @Test
    void shouldCompareEnumByName() {
        assertThat(matchEnum(POST.POST_TYPE, Filter.of(PostType.EVENT)).toCondition())
                .hasValueSatisfying(c -> assertThat(c).hasToString(POST.POST_TYPE.eq(PostType.EVENT.name()).toString()));
    }

    @Test
    void shouldApplyMandatoryValue() {
        assertThat(required(POST.BUILDING_ID, Filter.of(BUILDING_ID)).toCondition())
                .hasValueSatisfying(c -> assertThat(c).hasToString(POST.BUILDING_ID.eq(BUILDING_ID).toString()));
    }

    @Test
    void shouldFailInsteadOfWideningResultWhenMandatoryValueIsMissing() {
        assertThatThrownBy(() -> required(POST.BUILDING_ID, Filter.<UUID>empty()).toCondition())
                .isInstanceOf(IllegalApplicationStateException.class)
                .asInstanceOf(throwable(IllegalApplicationStateException.class))
                .extracting(AppException::getAppErrors, list(AppError.class))
                .singleElement()
                .satisfies(error -> {
                    assertThat(error.code()).isEqualTo(Code.MANDATORY_FILTER_MISSING);
                    assertThat(error.message()).contains(POST.BUILDING_ID.getName());
                });

        assertThatThrownBy(() -> required(POST.BUILDING_ID, (Filter<UUID>) null).toCondition())
                .isInstanceOf(IllegalApplicationStateException.class);
    }

    @Test
    void shouldMapPresentValueWithCustomMapper() {
        var criterion = when(Filter.of(PostType.ANNOUNCEMENT), type -> POST.POST_TYPE.ne(type.name()));

        assertThat(criterion.toCondition())
                .hasValueSatisfying(c -> assertThat(c).hasToString(POST.POST_TYPE.ne(PostType.ANNOUNCEMENT.name()).toString()));
    }

    @Test
    void shouldKeepOnlyPresentCriteriaInOrder() {
        var conditions = Criteria.of(
                required(POST.BUILDING_ID, Filter.of(BUILDING_ID)),
                match(POST.CREATED_BY, Filter.<UUID>empty()),
                matchEnum(POST.POST_TYPE, Filter.of(PostType.EVENT))
        );

        assertThat(conditions).hasSize(2);
        assertThat(conditions.getFirst()).hasToString(POST.BUILDING_ID.eq(BUILDING_ID).toString());
        assertThat(conditions.getLast()).hasToString(POST.POST_TYPE.eq(PostType.EVENT.name()).toString());
    }

    @Test
    void shouldReduceEmptyFilterToTrueCondition() {
        PredicateFilter empty = List::of;

        assertThat(empty.parseFilter().toString()).isEqualToIgnoringCase("true");
    }

    @Test
    void shouldExpressRelatedTableAsCorrelatedSubquery() {
        var condition = Related.existsIn(POLL_OPTION, POLL_OPTION.POLL_ID.eq(POLL.ID));

        assertThat(condition.toString())
                .contains("exists")
                .contains(POLL_OPTION.getName());
    }

    @Test
    void shouldUseAlwaysForConditionsWithoutARequestParam() {
        assertThat(always(POST.VISIBLE_TO.gt(NOW)).toCondition())
                .hasValueSatisfying(c -> assertThat(c).hasToString(POST.VISIBLE_TO.gt(NOW).toString()));
    }
}
