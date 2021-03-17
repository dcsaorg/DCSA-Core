package org.dcsa.core.query;

import org.dcsa.core.extendedrequest.JoinDescriptor;
import org.dcsa.core.extendedrequest.TableAndJoins;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.query.impl.DefaultDBEntityAnalysisBuilder;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Join;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;

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
         * @return The primary model class.  This is class referenced in the entity class's {@link org.dcsa.core.model.PrimaryModel}
         * annotation or the entity itself (the latter is absent).
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
         * not selectable ({@link QueryField#isSelectable()}.  However, they are required to have unique JSON names,
         * internal query names and (if selectable) select column names.
         *
         * @param queryField Register the query field.  If {@link QueryField#isSelectable()} returns true, then
         *                   the field will be extracted from the database query (using
         *                   {@link QueryField#getSelectColumnName()} as name).
         */
        DBEntityAnalysisBuilder<T> registerQueryField(QueryField queryField);

        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Table lhsJoinAlias, Table rhsModel);
        DBEntityAnalysisBuilder<T> joinOn(Join.JoinType joinType, Column lhsColumn, Column rhsColumn);
        DBEntityAnalysis<T> build();
    }

    interface DBEntityAnalysisJoinBuilder<T> {
        DBEntityAnalysisBuilder<T> onEquals(String lhsColumnName, String rhsColumnName);
        DBEntityAnalysisBuilder<T> onEquals(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName);
        DBEntityAnalysisBuilder<T> onFieldEquals(String lhsFieldName, String rhsFieldName);

        // Chain into another join - useful for "FROM A JOIN B (A.x=B.x) JOIN C (B.y=C.y)"-cases
        DBEntityAnalysisChainJoinBuilder<T> onEqualsThen(String lhsColumnName, String rhsColumnName);
        DBEntityAnalysisChainJoinBuilder<T> onEqualsThen(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName);
        DBEntityAnalysisChainJoinBuilder<T> onFieldEqualsThen(String lhsFieldName, String rhsFieldName);
    }

    interface DBEntityAnalysisChainJoinBuilder<T> {

        DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Table rhsTable);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Table rhsTable);
    }
}
