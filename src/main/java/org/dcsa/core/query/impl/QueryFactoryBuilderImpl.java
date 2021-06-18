package org.dcsa.core.query.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.FilterCondition;
import org.dcsa.core.extendedrequest.JoinDescriptor;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.extendedrequest.TableAndJoins;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.query.QueryFactoryBuilder;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.sql.*;
import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.Bindings;
import org.springframework.r2dbc.core.binding.MutableBindings;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor(staticName = "of")
public class QueryFactoryBuilderImpl<T> implements QueryFactoryBuilder<T>, QueryFactoryBuilder.FrozenBuilder<QueryFactoryBuilder<T>> {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final DBEntityAnalysis<T> dbEntityAnalysis;
    private final boolean selectDistinct;

    private final Function<SelectBuilder.SelectFromAndJoin, SelectBuilder.SelectFromAndJoin> limitOffset;
    private final Function<SelectBuilder.SelectOrdered, SelectBuilder.BuildSelect> order;
    private final R2dbcDialect r2dbcDialect;
    private final BindingInformation bindingInformation;

    public QueryFactoryBuilder<T> copyBuilder() {
        // Immutable, so we can just return this
        return this;
    }

    public QueryFactoryBuilder.FrozenBuilder<QueryFactoryBuilder<T>> freeze() {
        // Immutable, so we can just return this
        return this;
    }

    public QueryFactoryBuilder<T> distinct() {
        if (selectDistinct) {
            return this;
        }
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, true, limitOffset, order, r2dbcDialect, bindingInformation);
    }

    public QueryFactoryBuilder<T> limitOffset(Function<SelectBuilder.SelectFromAndJoin, SelectBuilder.SelectFromAndJoin> limitOffsetFunction) {
        if (limitOffsetFunction == limitOffset) {
            return this;
        }
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, selectDistinct, limitOffsetFunction, order, r2dbcDialect, bindingInformation);
    }

    public QueryFactoryBuilder<T> order(Function<SelectBuilder.SelectOrdered, SelectBuilder.BuildSelect> orderFunction) {
        if (orderFunction == order) {
            return this;
        }
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, selectDistinct, limitOffset, orderFunction, r2dbcDialect, bindingInformation);
    }

    public QueryFactoryBuilder<T> order(BiFunction<SelectBuilder.SelectOrdered, DBEntityAnalysis<T>, SelectBuilder.BuildSelect> orderFunction) {
        if (orderFunction == order) {
            return this;
        }
        return order((b) -> orderFunction.apply(b, dbEntityAnalysis));
    }

    public QueryFactoryBuilder<T> r2dbcDialect(R2dbcDialect newR2dbcDialect) {
        if (newR2dbcDialect == this.r2dbcDialect) {
            return this;
        }
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, selectDistinct, limitOffset, order, newR2dbcDialect, bindingInformation);
    }

    public ConditionBuilder<T> conditions() {
        if (r2dbcDialect == null) {
            throw new IllegalStateException("Missing r2dbcDialect parameter");
        }
        ConditionBuilderImpl builder = new ConditionBuilderImpl();
        builder.inheritedBindingInformation = bindingInformation;
        return builder;
    }

    private static void addAliases(Set<String> joinAliasInUse, QueryField fieldInUse, TableAndJoins tableAndJoins) {
        String joinAlias = fieldInUse.getTableJoinAlias();
        while (joinAlias != null) {
            JoinDescriptor descriptor = tableAndJoins.getJoinDescriptor(joinAlias);
            if (descriptor == null) {
                break;
            }
            String alias = descriptor.getJoinAliasId();
            if (!joinAliasInUse.add(alias)) {
                break;
            }
            joinAlias = descriptor.getDependentAlias();
        }
    }

    public AbstractQueryFactory<T> build() {
        BindingInformation binds = bindingInformation;
        if (binds == null) {
            binds = BindingInformation.EMPTY;
        }
        return doBuild(binds);
    }

    private AbstractQueryFactory<T> doBuild(BindingInformation bindingInformation) {
        Set<String> usedAliases = new HashSet<>();
        TableAndJoins tableAndJoins = dbEntityAnalysis.getTableAndJoins();
        List<Expression> expressions = dbEntityAnalysis.getAllSelectableFields().stream().map(queryField -> {
            addAliases(usedAliases, queryField, tableAndJoins);
            return queryField.getSelectColumn();
        }).collect(Collectors.toList());

        Bindings bindings;
        Condition condition;

        if (!bindingInformation.conditions.isEmpty()) {
            FilterCondition filterCondition = QueryGeneratorUtils.andAllFilters(bindingInformation.conditions, false);
            condition = filterCondition.computeCondition(r2dbcDialect);
            bindings = bindingInformation.bindings;
            for (QueryField queryField : bindingInformation.referencedQueryFields) {
                addAliases(usedAliases, queryField, tableAndJoins);
            }
        } else {
            condition = TrueCondition.INSTANCE;
            bindings = new Bindings();
        }

        return GenericQueryFactory.of(
                dbEntityAnalysis,
                selectDistinct,
                limitOffset,
                order,
                Set.of(usedAliases.toArray(EMPTY_STRING_ARRAY)),
                condition,
                expressions,
                new RenderContextFactory(r2dbcDialect),
                bindings
        );
    }

    private QueryFactoryBuilder<T> withBindingInformation(BindingInformation newBindingInformation) {
        if (Objects.equals(newBindingInformation, this.bindingInformation)) {
            return this;
        }
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, selectDistinct, limitOffset, order, r2dbcDialect, newBindingInformation);
    }

    @Data
    @RequiredArgsConstructor(staticName = "of")
    private static class BindingInformation {

        public static BindingInformation EMPTY = BindingInformation.of(
                new Bindings(),
                Collections.emptyList(),
                Collections.emptySet()
        );

        private final Bindings bindings;
        private final List<FilterCondition> conditions;
        private final Set<QueryField> referencedQueryFields;
    }

    @RequiredArgsConstructor
    private class ConditionBuilderImpl implements ConditionBuilder<T>, FrozenBuilder<ConditionBuilder<T>> {
        private BindingInformation inheritedBindingInformation;
        private List<FilterCondition> conditions = new ArrayList<>();
        private MutableBindings mutableBindings = new MutableBindings(r2dbcDialect.getBindMarkersFactory().create());
        // Shadows the QueryFactoryBuilderImpl.bindingInformation field by intention.
        private BindingInformation bindingInformation;
        private Set<QueryField> referencedQueryFields = new HashSet<>();
        private boolean isFrozen = false;

        public ComparisonBuilder<T> fieldByJsonName(String jsonName) {
            return withField(dbEntityAnalysis.getQueryFieldFromJSONName(jsonName));
        }

        public ComparisonBuilder<T> fieldByJavaField(String javaField) {
            return withField(dbEntityAnalysis.getQueryFieldFromJavaFieldName(javaField));
        }

        public QueryFactoryBuilder<T> withQuery() {
            if (!isFrozen) {
                freeze();
            }
            return withBindingInformation(bindingInformation);
        }

        private ComparisonBuilder<T> withField(QueryField queryField) {
            ConditionBuilderImpl conditionBuilder = this;
            if (isFrozen) {
                conditionBuilder = new ConditionBuilderImpl();
                conditionBuilder.inheritedBindingInformation = bindingInformation;
                conditionBuilder.referencedQueryFields.addAll(referencedQueryFields);
                conditionBuilder.conditions.addAll(conditions);
            }
            return new ComparisonBuilderImpl(conditionBuilder, queryField);
        }

        public FrozenBuilder<ConditionBuilder<T>> freeze() {
            if (isFrozen) {
                return this;
            }
            referencedQueryFields = Set.copyOf(referencedQueryFields);
            conditions = List.copyOf(conditions);
            Bindings bindings;
            if (inheritedBindingInformation != null) {
                bindings = inheritedBindingInformation.bindings.and(mutableBindings);
            } else {
                bindings = new Bindings().and(mutableBindings);
            }
            bindingInformation = BindingInformation.of(bindings, conditions, referencedQueryFields);
            mutableBindings = null;
            isFrozen = true;
            // We no longer need the inherited information as we copied all the data from it.  We might as well
            // remove the reference to enable garbage collection to clean up the parent if it
            // is no longer referenced.
            inheritedBindingInformation = null;
            return this;
        }

        public ConditionBuilder<T> copyBuilder() {
            if (!isFrozen) {
                freeze();
            }
            return this;
        }

        public AbstractQueryFactory<T> build() {
            if (!isFrozen) {
                freeze();
            }
            return doBuild(bindingInformation);
        }
    }

    @RequiredArgsConstructor
    private class ComparisonBuilderImpl implements ComparisonBuilder<T> {
        private final ConditionBuilderImpl conditionBuilder;
        private final QueryField queryField;

        @Override
        public ConditionBuilder<T> isNull() {
            return nullComparison(ComparisonType.EQ);
        }

        @Override
        public ConditionBuilder<T> isNotNull() {
            return nullComparison(ComparisonType.NEQ);
        }

        @Override
        public ConditionBuilder<T> greaterThan(Object value) {
            return comparison(ComparisonType.GT, value);
        }

        @Override
        public ConditionBuilder<T> greaterThanOrEqualTo(Object value) {
            return comparison(ComparisonType.GTE, value);
        }

        @Override
        public ConditionBuilder<T> equalTo(Object value) {
            return comparison(ComparisonType.EQ, value);
        }

        @Override
        public ConditionBuilder<T> lessThanOrEqualTo(Object value) {
            return comparison(ComparisonType.LTE, value);
        }

        @Override
        public ConditionBuilder<T> lessThan(Object value) {
            return comparison(ComparisonType.LT, value);
        }

        @Override
        public ConditionBuilder<T> oneOf(List<Object> values) {
            FilterCondition filterCondition = ComparisonType.EQ.multiValueCondition(queryField,
                    values.stream().map(v -> bindValue(queryField, v)).collect(Collectors.toList()));

            return finish(filterCondition);
        }

        private ConditionBuilder<T> nullComparison(ComparisonType comparison) {
            Condition condition = Conditions.isNull(comparison.defaultFieldConversion(queryField));
            if (comparison == ComparisonType.NEQ) {
                condition = condition.not();
            }
            return finish(InlineableFilterCondition.of(condition));
        }

        private Expression bindValue(QueryField queryField, Object value) {
            MutableBindings bindings = conditionBuilder.mutableBindings;
            BindMarker marker = bindings.nextMarker(queryField.getJsonName());
            bindings.bind(marker, value);
            return SQL.bindMarker(marker.getPlaceholder());
        }

        private ConditionBuilder<T> comparison(ComparisonType comparisonType, Object value) {
            FilterCondition filterCondition = comparisonType.singleNonNullValueCondition(queryField, bindValue(queryField, value));
            return finish(filterCondition);
        }

        private ConditionBuilder<T> finish(FilterCondition filterCondition) {
            conditionBuilder.conditions.add(filterCondition);
            conditionBuilder.referencedQueryFields.add(queryField);
            return conditionBuilder;
        }
    }
}
