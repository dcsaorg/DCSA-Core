package org.dcsa.core.query;

import org.dcsa.core.extendedrequest.JoinDescriptor;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.extendedrequest.QueryFieldConditionGenerator;
import org.dcsa.core.extendedrequest.TableAndJoins;
import org.dcsa.core.query.impl.DefaultDBEntityAnalysisBuilder;
import org.springframework.data.relational.core.sql.*;

import java.util.List;

public interface DBEntityAnalysis<T> {

    QueryField getQueryFieldFromJSONName(String jsonName) throws IllegalArgumentException;
    QueryField getQueryFieldFromJavaFieldName(String javaFieldName) throws IllegalArgumentException;
    QueryField getQueryFieldFromSelectName(String selectColumnName) throws IllegalArgumentException;
    List<QueryField> getAllSelectableFields();
    TableAndJoins getTableAndJoins();

    static <T> DBEntityAnalysisBuilder<T> builder(Class<T> entityType) {
        return new DefaultDBEntityAnalysisBuilder<>(entityType);
    }

    interface DBEntityAnalysisBuilder<T> {
        /**
         *
         * @return The primary model class.
         */
        Class<?> getPrimaryModelClass();

        /**
         *
         * @return The Table for the Primary model class.  This is the table using in the FROM part of the query.
         */
        Table getPrimaryModelTable();

        JoinDescriptor getJoinDescriptor(String aliasId);
        JoinDescriptor getJoinDescriptor(Class<?> modelClass);

        DBEntityAnalysisBuilder<T> loadFieldsAndJoinsFromModel();


        DBEntityAnalysisBuilder<T> registerJoinDescriptor(JoinDescriptor joinDescriptor);

        /**
         * Field registered via this method are <em>not</em> required to exist on any Java class provided they are
         * not selectable ({@link QueryField#isSelectable()}).  However, they are required to have unique JSON names,
         * internal query names and (if selectable) select column names.
         *
         * @param queryField Register the query field.  If {@link QueryField#isSelectable()} returns true, then
         *                   the field will be extracted from the database query (using
         *                   {@link QueryField#getSelectColumn()} as name).
         */
        DBEntityAnalysisBuilder<T> registerQueryField(QueryField queryField);
        DBEntityAnalysisBuilder<T> registerQueryFieldAlias(String jsonName, String jsonNameAlias);

        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, TableLike lhsTable, TableLike rhsTable);
        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, TableLike lhsTable, TableLike rhsTable, Class<?> rhsModel);
        DBEntityAnalysisBuilder<T> joinOn(Join.JoinType joinType, Column lhsColumn, Column rhsColumn);
        DBEntityAnalysisWithTableBuilder<T> onTable(String alias);
        DBEntityAnalysisWithTableBuilder<T> onTable(Class<?> model);
        DBEntityAnalysisWithTableBuilder<T> onTable(TableLike table);
        DBEntityAnalysis<T> build();
    }

    interface DBEntityAnalysisJoinBuilder<T> extends ConditionBuilder<T, DBEntityAnalysisWithTableBuilder<T>> {

    }

    interface ConditionBuilder<T, R> {
        ConditionChainBuilder<T, R> onEquals(String lhsColumnName, String rhsColumnName);
        ConditionChainBuilder<T, R> onEquals(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName);
        ConditionChainBuilder<T, R> onFieldEquals(String lhsFieldName, String rhsFieldName);

        R onEqualsThen(String lhsColumnName, String rhsColumnName);
        R onEqualsThen(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName);
        R onFieldEqualsThen(String lhsFieldName, String rhsFieldName);
    }

    /*
     * Note it is not possible to use nesting to affect precedence, so default SQL precedence applies.
     *
     * As a consequence, this interface only supports "Disjunctive Normal Form".
     */
    interface ConditionChainBuilder<T, R> {
        ConditionBuilder<T, R> and();
        ConditionBuilder<T, R> or();
    }

    interface DBEntityAnalysisWithTableBuilder<T> {

        DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> chainJoin(TableLike rhsTable);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, TableLike rhsTable);

        default DBEntityAnalysisBuilder<T> registerQueryFieldFromField(String fieldName) {
            return this.registerQueryFieldFromField(fieldName, null);
        }
        default DBEntityAnalysisBuilder<T> registerQueryFieldFromField(String fieldName, QueryFieldConditionGenerator conditionGenerator) {
          return registerQueryFieldFromField(fieldName, "", conditionGenerator);
        }
        DBEntityAnalysisBuilder<T> registerQueryFieldFromField(String fieldName, String jsonPrefix, QueryFieldConditionGenerator conditionGenerator);
        default DBEntityAnalysisBuilder<T> registerQueryField(SqlIdentifier columnName, String jsonName, Class<?> valueType) {
            return this.registerQueryField(columnName, jsonName, valueType, null);
        }
        DBEntityAnalysisBuilder<T> registerQueryField(SqlIdentifier columnName, String jsonName, Class<?> valueType, QueryFieldConditionGenerator conditionGenerator);
        DBEntityAnalysisBuilder<T> registerQueryFieldAlias(String jsonName, String jsonNameAlias);

        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromFieldThen(String fieldName) {
            registerQueryFieldFromField(fieldName);
            return this;
        }

        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromFieldThen(String fieldName, String jsonPrefix) {
          registerQueryFieldFromField(fieldName, jsonPrefix, null);
          return this;
        }

        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldThen(SqlIdentifier columnName, String jsonName, Class<?> valueType) {
            registerQueryField(columnName, jsonName, valueType);
            return this;
        }

        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromFieldThen(String fieldName, QueryFieldConditionGenerator conditionGenerator) {
            registerQueryFieldFromField(fieldName, conditionGenerator);
            return this;
        }

        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldThen(SqlIdentifier columnName, String jsonName, Class<?> valueType, QueryFieldConditionGenerator conditionGenerator) {
            registerQueryField(columnName, jsonName, valueType, conditionGenerator);
            return this;
        }

        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldAliasThen(String jsonName, String jsonNameAlias) {
            registerQueryFieldAlias(jsonName, jsonNameAlias);
            return this;
        }

    }
}
