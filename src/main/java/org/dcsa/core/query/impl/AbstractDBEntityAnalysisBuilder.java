package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.JoinDescriptor;
import org.dcsa.core.extendedrequest.QueryFieldConditionGenerator;
import org.dcsa.core.extendedrequest.QueryFields;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Join;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.BiFunction;

public abstract class AbstractDBEntityAnalysisBuilder<T> implements DBEntityAnalysis.DBEntityAnalysisBuilder<T> {

    protected abstract String getColumnName(Class<?> clazz, String fieldName, String annotationVariablePrefix);

    protected abstract Table getTableForModel(Class<?> model, String alias);

    protected Table getTableFor(String alias) {
        JoinDescriptor joinDescriptor = getJoinDescriptor(alias);
        if (joinDescriptor == null) {
            return getPrimaryModelTable();
        }
        return joinDescriptor.getRHSTable();
    }

    protected Table getTableFor(Class<?> lhsModel) {
        JoinDescriptor joinDescriptor = getJoinDescriptor(lhsModel);
        if (joinDescriptor == null) {
            return getPrimaryModelTable();
        }
        return joinDescriptor.getRHSTable();
    }


    protected abstract Class<?> getModelFor(String aliasId);

    protected abstract Class<?> getModelFor(Table table);

    protected DBEntityAnalysis.DBEntityAnalysisBuilder<T> joinOnImpl(Join.JoinType joinType, Column lhsColumn, Column rhsColumn, Class<?> rhsModel) {
        Table lhsTable = lhsColumn.getTable();
        checkJoinType(joinType, rhsModel);
        if (lhsTable == null) {
            throw new IllegalArgumentException("lhsColumn must have a non-null getTable()");
        }
        if (rhsColumn.getTable() == null) {
            throw new IllegalArgumentException("rhsColumn must have a non-null getTable()");
        }
        String lhsAlias = ReflectUtility.getAliasId(lhsTable);
        return registerJoinDescriptor(SimpleJoinDescriptor.of(joinType, lhsColumn, rhsColumn, rhsModel, lhsAlias));
    }

    protected DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> joinOnThenImpl(Join.JoinType joinType, Column lhsColumn, Column rhsColumn, Class<?> rhsModel) {
        joinOnImpl(joinType, lhsColumn, rhsColumn, rhsModel);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, Objects.requireNonNull(rhsColumn.getTable()), rhsModel);
    }


    public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onTable(String alias) {
        Class<?> model = getModelFor(alias);
        Table table = getTableFor(alias);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, table, model);
    }

    public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onTable(Class<?> model) {
        Table table = getTableFor(model);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, table, model);
    }

    public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onTable(Table table) {
        Class<?> model = getModelFor(table);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, table, model);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel) {
        Table rhsTable = getTableForModel(rhsModel, null);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsModel), rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel, String rhsJoinAlias) {
        Table rhsTable = getTableForModel(rhsModel, rhsJoinAlias);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsModel), rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel) {
        Table rhsTable = getTableForModel(rhsModel, null);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsJoinAlias), rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel, String rhsJoinAlias) {
        Table rhsTable = getTableForModel(rhsModel, rhsJoinAlias);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsJoinAlias), rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Table lhsTable, Table rhsTable) {
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, lhsTable, rhsTable, null);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Table lhsTable, Table rhsTable, Class<?> rhsModel) {
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, lhsTable, rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> joinOn(Join.JoinType joinType, Column lhsColumn, Column rhsColumn) {
        return joinOnImpl(joinType, lhsColumn, rhsColumn, null);
    }


    protected void checkJoinType(Join.JoinType joinType, Class<?> modelType) {
        /* Fail-fast on unsupported joins */
        switch (joinType) {
            case JOIN:
            case LEFT_OUTER_JOIN:
                break;
            case RIGHT_OUTER_JOIN:
                if (modelType == null) {
                    throw new UnsupportedOperationException("Cannot generate query: Please replace RIGHT JOINs with LEFT JOINs");
                }
                throw new UnsupportedOperationException("Cannot generate query for " + modelType.getSimpleName()
                        + ": Please replace RIGHT JOINs with LEFT JOINs");
            default:
                /* Remember to update JoinDescriptor.apply */
                if (modelType == null) {
                    throw new IllegalArgumentException("Unsupported joinType: " + joinType.name());
                }
                throw new IllegalArgumentException("Unsupported joinType: " + joinType.name() + " on " + modelType.getSimpleName());
        }
    }

    @RequiredArgsConstructor(staticName = "of")
    protected static class DBEntityAnalysisJoinBuilderImpl<T> implements DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> {
        private final AbstractDBEntityAnalysisBuilder<T> builder;
        private final Join.JoinType joinType;
        private final Table lhsTable;
        private final Table rhsTable;
        private final Class<?> rhsModel;

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> onEquals(String lhsColumnName, String rhsColumnName) {
            return onEqualsImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> onEquals(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName) {
            return onEqualsImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onEqualsThen(String lhsColumnName, String rhsColumnName) {
            return onEqualsThenImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onEqualsThen(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName) {
            return onEqualsThenImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        private <P> DBEntityAnalysis.DBEntityAnalysisBuilder<T> onEqualsImpl(P lhs, P rhs, BiFunction<P, Table, Column> toColumn) {
            Column lhsColumn = toColumn.apply(lhs, lhsTable);
            Column rhsColumn = toColumn.apply(rhs, rhsTable);
            return builder.joinOnImpl(joinType, lhsColumn, rhsColumn, rhsModel);
        }

        private <P> DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onEqualsThenImpl(P lhs, P rhs, BiFunction<P, Table, Column> toColumn) {
            Column lhsColumn = toColumn.apply(lhs, lhsTable);
            Column rhsColumn = toColumn.apply(rhs, rhsTable);
            return builder.joinOnThenImpl(joinType, lhsColumn, rhsColumn, rhsModel);
        }

        private <B> B onFieldEqualsImpl(String lhsFieldName, String rhsFieldName, BiFunction<String, String, B> impl) {
            Class<?> lhsModel = builder.getModelFor(lhsTable);
            String lhsColumnName;
            String rhsColumnName;
            if (lhsModel == null) {
                throw new UnsupportedOperationException("Cannot use field based join as the join is not based on a " +
                        "Model class (LHS side missing). Please use join/chainJoin with a Class parameter if you need this feature.");
            }
            if (rhsModel == null) {
                throw new UnsupportedOperationException("Cannot use field based join as the join is not based on a " +
                        "Model class (RHS side missing). Please use join/chainJoin with a Class parameter if you need this feature.");
            }
            try {
                lhsColumnName = ReflectUtility.transformFromFieldNameToColumnName(lhsModel, lhsFieldName);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Unknown field \"" + lhsFieldName + "\" on model " + lhsModel.getSimpleName() + " (khs)");
            }
            try {
                rhsColumnName = ReflectUtility.transformFromFieldNameToColumnName(rhsModel, rhsFieldName);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Unknown field \"" + rhsFieldName + "\" on model " + rhsModel.getSimpleName() + " (rhs)");
            }
            return impl.apply(lhsColumnName, rhsColumnName);
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> onFieldEquals(String lhsFieldName, String rhsFieldName) {
            return onFieldEqualsImpl(lhsFieldName, rhsFieldName, this::onEquals);
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onFieldEqualsThen(String lhsFieldName, String rhsFieldName) {
            return onFieldEqualsImpl(lhsFieldName, rhsFieldName, this::onEqualsThen);
        }
    }

    @RequiredArgsConstructor(staticName = "of")
    protected static class DBEntityAnalysisWithTableBuilderImpl<T> implements DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> {

        private final AbstractDBEntityAnalysisBuilder<T> builder;
        private final Table lhsTable;
        private final Class<?> lhsModel;

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel) {
            return chainJoin(Join.JoinType.JOIN, rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel, String rhsJoinAlias) {
            return chainJoin(Join.JoinType.JOIN, rhsModel, rhsJoinAlias);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Table rhsTable) {
            return chainJoin(Join.JoinType.JOIN, rhsTable);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel) {
            return chainJoinImpl(joinType, builder.getTableForModel(rhsModel, null), rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel, String rhsJoinAlias) {
            return chainJoinImpl(joinType, builder.getTableForModel(rhsModel, rhsJoinAlias), rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Table rhsTable) {
            return chainJoinImpl(joinType, rhsTable, null);
        }

        private DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoinImpl(Join.JoinType joinType, Table rhsTable, Class<?> rhsModel) {
            return DBEntityAnalysisJoinBuilderImpl.of(builder, joinType, lhsTable, rhsTable, rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> registerQueryFieldAlias(String jsonName, String jsonNameAlias) {
            return builder.registerQueryFieldAlias(jsonName, jsonNameAlias);
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> registerQueryFieldFromField(String fieldName, QueryFieldConditionGenerator conditionGenerator) {
            Field field;
            if (lhsModel == null) {
                throw new UnsupportedOperationException("Cannot use field based field registration as there is no model"
                        + " associated with the table");
            }
            try {
                field = ReflectUtility.getDeclaredField(lhsModel, fieldName);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("The model " + lhsModel.getSimpleName() + " does not have a field \""
                        + fieldName + "\"");
            }
            return builder.registerQueryField(QueryFields.queryFieldFromFieldWithSelectPrefix(
                    field,
                    lhsTable,
                    false,
                    "",
                    conditionGenerator
            ));
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> registerQueryField(SqlIdentifier columnName, String jsonName, Class<?> valueType, QueryFieldConditionGenerator conditionGenerator) {
            Column column = Column.create(columnName, lhsTable);
            return builder.registerQueryField(QueryFields.nonSelectableQueryField(column, jsonName, valueType, conditionGenerator));
        }

    }
}
