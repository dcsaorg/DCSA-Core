package org.dcsa.core.query;

import org.dcsa.core.extendedrequest.*;
import org.dcsa.core.query.impl.DefaultDBEntityAnalysisBuilder;
import org.springframework.data.relational.core.sql.*;

import java.util.Collection;
import java.util.List;

public interface DBEntityAnalysis<T> {

    QueryField getQueryFieldFromJSONName(String jsonName) throws IllegalArgumentException;
    QueryField getQueryFieldFromJavaFieldName(String javaFieldName) throws IllegalArgumentException;
    QueryField getQueryFieldFromSelectName(String selectColumnName) throws IllegalArgumentException;
    QueryFieldRestriction getQueryFieldRestriction(QueryField queryField) throws IllegalArgumentException;
    Collection<QueryField> getQueryFields();
    List<QueryField> getAllSelectableFields();
    TableAndJoins getTableAndJoins();

    static <T> DBEntityAnalysisBuilder<T> builder(Class<T> entityType) {
        return new DefaultDBEntityAnalysisBuilder<>(entityType);
    }

  /**
   * A builder to generate an instance of {@link DBEntityAnalysis}
   *
   * The builder can be used to provide custom query fields (query parameters mapped to SQL conditions),
   * setup SQL joins and set defaults or validation rules on query fields (both custom and default ones).
   *
   * Custom query fields are usually best registered via {@link DBEntityAnalysisWithTableBuilder} and its
   * methods for doing that.
   *
   * @param <T> The model class of the root entity
   */
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

        DBEntityAnalysisBuilder<T> registerRestrictionOnQueryField(String jsonName, QueryFieldRestriction queryFieldRestriction);

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

        R endCondition();
    }

    interface DBEntityAnalysisWithTableBuilder<T> {

        DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> chainJoin(TableLike rhsTable);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, TableLike rhsTable);

        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromField(String fieldName) {
            return this.registerQueryFieldFromField(fieldName, "", null, null);
        }
        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromField(String fieldName, String jsonPrefix) {
          return this.registerQueryFieldFromField(fieldName, jsonPrefix, null, null);
        }
        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromField(String fieldName, QueryFieldRestriction queryFieldRestriction) {
          return this.registerQueryFieldFromField(fieldName, "", queryFieldRestriction, null);
        }
        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromField(String fieldName, String jsonPrefix, QueryFieldRestriction queryFieldRestriction) {
          return this.registerQueryFieldFromField(fieldName, jsonPrefix, queryFieldRestriction, null);
        }
        default DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromField(String fieldName, QueryFieldConditionGenerator conditionGenerator) {
          return registerQueryFieldFromField(fieldName, "", null, conditionGenerator);
        }
        DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromField(String fieldName, String jsonPrefix, QueryFieldRestriction queryFieldRestriction, QueryFieldConditionGenerator conditionGenerator);
        default DBEntityAnalysisWithTableBuilder<T> registerQueryField(SqlIdentifier columnName, String jsonName, Class<?> valueType) {
            return this.registerQueryField(columnName, jsonName, valueType, null);
        }
        DBEntityAnalysisWithTableBuilder<T> registerQueryField(SqlIdentifier columnName, String jsonName, Class<?> valueType, QueryFieldConditionGenerator conditionGenerator);
        DBEntityAnalysisWithTableBuilder<T> registerQueryFieldAlias(String jsonName, String jsonNameAlias);

        DBEntityAnalysisWithTableBuilder<T> registerRestrictionOnQueryField(String jsonName, QueryFieldRestriction queryFieldRestriction);

        default DBEntityAnalysisWithTableBuilder<T> onTable(String alias) {
          return finishTable().onTable(alias);
        }

        default DBEntityAnalysisWithTableBuilder<T> onTable(Class<?> model) {
          return finishTable().onTable(model);
        }

        default DBEntityAnalysisWithTableBuilder<T> onTable(TableLike table) {
          return finishTable().onTable(table);
        }

        DBEntityAnalysisBuilder<T> finishTable();
    }
}
