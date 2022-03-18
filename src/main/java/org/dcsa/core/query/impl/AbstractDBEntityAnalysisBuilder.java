package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.JoinDescriptor;
import org.dcsa.core.extendedrequest.QueryFieldConditionGenerator;
import org.dcsa.core.extendedrequest.QueryFields;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.sql.*;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.BiFunction;

public abstract class AbstractDBEntityAnalysisBuilder<T> implements DBEntityAnalysis.DBEntityAnalysisBuilder<T> {

    protected abstract String getColumnName(Class<?> clazz, String fieldName, String annotationVariablePrefix);

    protected abstract Table getTableForModel(Class<?> model, String alias);

    protected TableLike getTableFor(String alias) {
        JoinDescriptor joinDescriptor = getJoinDescriptor(alias);
        if (joinDescriptor == null) {
            return getPrimaryModelTable();
        }
        return joinDescriptor.getRHSTable();
    }

    protected TableLike getTableFor(Class<?> lhsModel) {
        JoinDescriptor joinDescriptor = getJoinDescriptor(lhsModel);
        if (joinDescriptor == null) {
            return getPrimaryModelTable();
        }
        return joinDescriptor.getRHSTable();
    }

    protected abstract Class<?> getModelFor(String aliasId);

    protected abstract Class<?> getModelFor(TableLike table);

    private static Condition columnsAreEqual(Column lhsColumn, Column rhsColumn) {
      if (lhsColumn.getTable() == null) {
        throw new IllegalArgumentException("lhsColumn must have a non-null getTable()");
      }
      if (rhsColumn.getTable() == null) {
        throw new IllegalArgumentException("rhsColumn must have a non-null getTable()");
      }
      return Conditions.isEqual(lhsColumn, rhsColumn);
    }

    protected DBEntityAnalysis.DBEntityAnalysisBuilder<T> joinOnImpl(Join.JoinType joinType, Condition condition, TableLike lhsTable, TableLike rhsTable, Class<?> rhsModel) {
        checkJoinType(joinType, rhsModel);
        String lhsAlias = ReflectUtility.getAliasId(lhsTable);
        return registerJoinDescriptor(SimpleJoinDescriptor.of(joinType, rhsTable, rhsModel, condition, lhsAlias));
    }

    protected DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> joinOnThenImpl(Join.JoinType joinType, Condition condition, TableLike lhsTable, TableLike rhsTable, Class<?> rhsModel) {
        joinOnImpl(joinType, condition, lhsTable, rhsTable, rhsModel);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, Objects.requireNonNull(rhsTable), rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onTable(String alias) {
        Class<?> model = getModelFor(alias);
        TableLike table = getTableFor(alias);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, table, model);
    }

    public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onTable(Class<?> model) {
        TableLike table = getTableFor(model);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, table, model);
    }

    public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onTable(TableLike table) {
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

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, TableLike lhsTable, TableLike rhsTable) {
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, lhsTable, rhsTable, null);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, TableLike lhsTable, TableLike rhsTable, Class<?> rhsModel) {
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, lhsTable, rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> joinOn(Join.JoinType joinType, Column lhsColumn, Column rhsColumn) {
        Condition condition = columnsAreEqual(lhsColumn, rhsColumn);
        return joinOnImpl(joinType, condition, Objects.requireNonNull(lhsColumn.getTable()), Objects.requireNonNull(rhsColumn.getTable()), null);
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

    @RequiredArgsConstructor
    private enum ConditionAggregator {
      FIRST_CONDITION((a, b) -> { assert a == null; return b; }),
      AND_CONDITION(Condition::and),
      OR_CONDITION(Condition::or),
      ;
      private final BiFunction<Condition, Condition, Condition> aggregator;

      public Condition merge(Condition lhs, Condition rhs) {
        return aggregator.apply(lhs, rhs);
      }
    }

    @RequiredArgsConstructor(staticName = "of")
    protected static class DBEntityAnalysisJoinBuilderImpl<T> implements DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> {
        private final AbstractDBEntityAnalysisBuilder<T> builder;
        private final Join.JoinType joinType;
        private final TableLike lhsTable;
        private final TableLike rhsTable;
        private final Class<?> rhsModel;
        private final Condition condition;
        private final ConditionAggregator currentAggregator;

        public DBEntityAnalysis.ConditionChainBuilder<T, DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T>> onEquals(String lhsColumnName, String rhsColumnName) {
            return onEqualsImpl(lhsColumnName, rhsColumnName, TableLike::column);
        }

        public DBEntityAnalysis.ConditionChainBuilder<T, DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T>> onEquals(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName) {
            return onEqualsImpl(lhsColumnName, rhsColumnName, TableLike::column);
        }

        public DBEntityAnalysis.ConditionChainBuilder<T, DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T>> onFieldEquals(String lhsFieldName, String rhsFieldName) {
            return onFieldEqualsImpl(lhsFieldName, rhsFieldName, this::onEquals);
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onEqualsThen(String lhsColumnName, String rhsColumnName) {
            return onEqualsThenImpl(lhsColumnName, rhsColumnName, TableLike::column);
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onEqualsThen(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName) {
            return onEqualsThenImpl(lhsColumnName, rhsColumnName, TableLike::column);
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onFieldEqualsThen(String lhsFieldName, String rhsFieldName) {
            return onFieldEqualsImpl(lhsFieldName, rhsFieldName, this::onEqualsThen);
        }

        private <P> ConditionChainBuilderImpl<T, DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T>> onEqualsImpl(P lhs, P rhs, BiFunction<TableLike, P, Column> toColumn) {
            Column lhsColumn = toColumn.apply(lhsTable, lhs);
            Column rhsColumn = toColumn.apply(rhsTable, rhs);
            Condition newCondition = columnsAreEqual(lhsColumn, rhsColumn);
            return (nextAggregator) -> of(
              builder,
              joinType,
              lhsTable,
              rhsTable,
              rhsModel,
              this.currentAggregator.merge(this.condition, newCondition),
              nextAggregator
            );
        }

        private <P> DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onEqualsThenImpl(P lhs, P rhs, BiFunction<TableLike, P, Column> toColumn) {
            Column lhsColumn = toColumn.apply(lhsTable, lhs);
            Column rhsColumn = toColumn.apply(rhsTable, rhs);
            Condition newCondition = columnsAreEqual(lhsColumn, rhsColumn);
            return builder.joinOnThenImpl(joinType, newCondition, lhsTable, rhsTable, rhsModel);
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

        public static <T> DBEntityAnalysisJoinBuilderImpl<T> of(AbstractDBEntityAnalysisBuilder<T> builder,
                                                                Join.JoinType joinType,
                                                                TableLike lhsTable,
                                                                TableLike rhsTable,
                                                                Class<?> rhsModel) {
          return of(builder, joinType, lhsTable, rhsTable, rhsModel, null, ConditionAggregator.FIRST_CONDITION);
        }
    }

    @FunctionalInterface
    private interface ConditionChainBuilderImpl<T, R> extends DBEntityAnalysis.ConditionChainBuilder<T, R> {

      default DBEntityAnalysis.ConditionBuilder<T, R> and() {
        return chainImpl(ConditionAggregator.AND_CONDITION);
      }

      default DBEntityAnalysis.ConditionBuilder<T, R> or() {
        return chainImpl(ConditionAggregator.OR_CONDITION);
      }

      DBEntityAnalysis.ConditionBuilder<T, R> chainImpl(ConditionAggregator conditionAggregator);
    }

    @RequiredArgsConstructor(staticName = "of")
    protected static class DBEntityAnalysisWithTableBuilderImpl<T> implements DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> {

        private final AbstractDBEntityAnalysisBuilder<T> builder;
        private final TableLike lhsTable;
        private final Class<?> lhsModel;

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel) {
            return chainJoin(Join.JoinType.JOIN, rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel, String rhsJoinAlias) {
            return chainJoin(Join.JoinType.JOIN, rhsModel, rhsJoinAlias);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(TableLike rhsTable) {
            return chainJoin(Join.JoinType.JOIN, rhsTable);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel) {
            return chainJoinImpl(joinType, builder.getTableForModel(rhsModel, null), rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel, String rhsJoinAlias) {
            return chainJoinImpl(joinType, builder.getTableForModel(rhsModel, rhsJoinAlias), rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, TableLike rhsTable) {
            return chainJoinImpl(joinType, rhsTable, null);
        }

        private DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoinImpl(Join.JoinType joinType, TableLike rhsTable, Class<?> rhsModel) {
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
            return builder.registerQueryField(QueryFields.nonSelectableQueryField(lhsTable.column(columnName), jsonName, valueType, conditionGenerator));
        }

    }
}
