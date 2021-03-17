package org.dcsa.core.models;

import org.dcsa.core.extendedrequest.*;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Join;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;

public class CitySpecificExtendedRequest extends ExtendedRequest<City> {
    public CitySpecificExtendedRequest(ExtendedParameters extendedParameters) {
        super(extendedParameters, City.class);
    }

    protected DBEntityAnalysis.DBEntityAnalysisBuilder<City> prepareDBEntityAnalysis() {
        DBEntityAnalysis.DBEntityAnalysisBuilder<City> builder = super.prepareDBEntityAnalysis();
        Table cityTable = builder.getPrimaryModelTable();
        Table countryTable = Table.create(SqlIdentifier.unquoted(ReflectUtility.getTableName(County.class))).as(SqlIdentifier.unquoted("c"));

        JoinDescriptor joinDescriptor = SimpleJoinDescriptor.of(
                Join.JoinType.JOIN,
                Column.create(SqlIdentifier.unquoted("country_id"), cityTable),
                Column.create(SqlIdentifier.unquoted("id"), countryTable),
                null
        );
        return builder
                .registerJoinDescriptor(joinDescriptor)
                .registerQueryField(QueryFields.nonSelectableQueryField(
                        "c", "country_name", "cn", String.class
                ));
    }
}
