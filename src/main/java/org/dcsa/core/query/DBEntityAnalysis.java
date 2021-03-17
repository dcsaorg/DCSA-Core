package org.dcsa.core.query;

import org.dcsa.core.extendedrequest.JoinDescriptor;
import org.dcsa.core.extendedrequest.TableAndJoins;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.query.impl.DefaultDBEntityAnalysisBuilder;
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

        /*
        DBEntityAnalysisJoinBuilder<T> joinWith(Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> joinWith(Class<?> lhsModel, Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> joinWith(Class<?> lhsModel, Class<?> rhsModel, String rhsJoinAlias);
        DBEntityAnalysisJoinBuilder<T> joinWith(String lhsJoinAlias, Class<?> rhsModel);
        DBEntityAnalysisJoinBuilder<T> joinWith(String lhsJoinAlias, Class<?> rhsModel, String rhsJoinAlias);
         */
        DBEntityAnalysis<T> build();
    }

    /*
    interface DBEntityAnalysisJoinBuilder<T> {
        DBEntityAnalysis<T> onEquals(String lhsField, String rhsField);
    }
     */
}
