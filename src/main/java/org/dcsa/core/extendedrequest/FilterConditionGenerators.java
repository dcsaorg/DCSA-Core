package org.dcsa.core.extendedrequest;

import org.dcsa.core.exception.GetException;
import org.springframework.data.relational.core.sql.Expression;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class FilterConditionGenerators {

    static final QueryFieldConditionGenerator IN_COMMA_SEPARATED_LIST = new QueryFieldConditionGenerator() {
        @Override
        public FilterCondition generateCondition(QueryField queryField, ComparisonType comparisonType, String fieldAttribute, List<String> queryParamValues, Function<String, Expression> value2BindVariable) {
            if (comparisonType != ComparisonType.EQ && comparisonType != ComparisonType.NEQ) {
                throw new GetException("Cannot use attribute (operator) " + fieldAttribute + " on " + queryField.getJsonName()
                        + ": The query parameter can only be used with strictly equal or not-equal relations");
            }

            List<Expression> allValues = queryParamValues.stream().flatMap(v -> Arrays.stream(v.split(",")))
                    .peek(v -> {
                                if ("NULL".equals(v)) {
                                    // "X IN (NULL)" is not the same as "X IS NULL" and it is not important to support it
                                    // right now.
                                    // Note: This remark assumes that "NULL" is mapped to a literal SQL null rather than
                                    // the string 'NULL'.  The assumption is to match how we handle NULL inside the
                                    // QueryParameterParser.
                                    throw new GetException("Support for NULL in " + queryField.getJsonName()
                                            + " is missing.");
                                }
                            }
                    ).map(value2BindVariable)
                    .collect(Collectors.toList());

            return comparisonType.multiValueCondition(queryField, allValues);
        }
    };
}
