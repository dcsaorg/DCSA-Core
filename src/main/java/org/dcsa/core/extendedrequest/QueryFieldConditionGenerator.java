package org.dcsa.core.extendedrequest;

import org.springframework.data.relational.core.sql.Expression;

import org.dcsa.core.query.DBEntityAnalysis;

import java.util.List;
import java.util.function.Function;

public abstract class QueryFieldConditionGenerator {

    /* package-private, the API should not be implemented outside DCSA-Core for now
     *
     * The API is sufficient to provide the inCommaSeparatedList feature, which is
     * what we need for now. However, it is subpar with the QueryParameterParser.
     * Notably the value2BindVariable parameter feels a bit "iffy", you are
     * forced into the pre-defined set of field attributes for ComparisonType,
     * etc.
     *
     * Fixing that is left for another day because we might not need that
     * flexibility.
     */
    QueryFieldConditionGenerator() {}

    /**
     * Generates the condition for this query field for a concrete request
     * <p>
     * This method will be called whenever the query field is used as a query parameter.  Note it can be called multiple
     * times for a given request if the field attributes differ and the results will be AND'ed together.
     * <p>
     * The implementation should convert the input into a {@link FilterCondition} using the following over all steps:
     *
     * <ol>
     *     <li>The implementation should check it is defined for the given comparison type. If not, throw an exception
     *         that will cause an HTTP Bad Request error.</li>
     *     <li>All values from queryParamValues that go into the query should be passed through the value2BindVariable
     *         function to get the bind variable to insert the value there. The string value passed there must be
     *         convertable into the underlying type of this query field (unless the comparison type forces the type into
     *         strings - see {@link ComparisonType#isConvertToString()})</li>
     *     <li>The bind variable(s) from the previous step should be passed to methods like
     *     {@link ComparisonType#singleNonNullValueCondition(QueryField, Expression)} or
     *     {@link ComparisonType#multiValueCondition(QueryField, List)} to generate the condition</li>
     *
     * </ol>
     *
     * @param queryField         The query field this generator should generate the condition for.
     * @param comparisonType     The comparison type used (defaults to {@link ComparisonType#EQ}). The code is not required to use this
     *                           comparison type - it is just what the engine thought would be used in the default context.
     *                           This generally mirrors the fieldAttribute except you cannot tell whether the choice was explicit
     *                           or implicit.
     *                           If the method does not support the given comparison type it should throw an exception that will
     *                           cause an HTTP 400 Bad Request response.
     * @param fieldAttribute     The field attribute used on the attribute (defaults to null when there is no explicit attribute).
     *                           This uses the exact case as provided by the client.
     * @param queryParamValues   The values passed as query parameters (as-is) for this choice of field attribute.  The code
     *                           may preprocess the values however it likes (but it may need to copy the list to do so as
     *                           the list itself is immutable).
     * @param value2BindVariable A function that maps a single query parameter value into the underlying type needed in the database.
     * @return The filterCondition that represents the SQL condition.
     */
    public abstract FilterCondition generateCondition(QueryField queryField,
                                                      ComparisonType comparisonType,
                                                      String fieldAttribute,
                                                      List<String> queryParamValues,
                                                      Function<String, Expression> value2BindVariable
    );

    /**
     * QueryFieldConditionGenerator that treats the Query field as a comma separated list
     *
     * The resulting QueryFieldConditionGenerator will treat the QueryField as a
     * comma-separated list of values.  The values will be used to generate the
     * equivalent of a "X IN (LIST)"-clause (or "X NOT IN (LIST)" if the ":neq"
     * attribute is used).  As an example, given the query parameter is:
     *
     * <code>q=A,B</code>
     *
     * Then by using this condition generator, the query will have a WHERE
     * clause containing:
     *
     * <code>q IN ('A', 'B')</code>
     *
     * (Literal values have been using to keep the example simple.  The
     * actual result will rely on bind variables.)
     *
     * The return value can be used together with {@link DBEntityAnalysis.DBEntityAnalysisWithTableBuilder#registerQueryFieldFromField(String, QueryFieldConditionGenerator)}
     * or similar methods to register a custom query field with this comma-list
     * interpretation.
     *
     * Note that the field attributes will be restricted to ":eq", ":neq" for the
     * query field.
     */
    public static QueryFieldConditionGenerator inCommaSeparatedList() {
        return FilterConditionGenerators.IN_COMMA_SEPARATED_LIST;
    }
}
