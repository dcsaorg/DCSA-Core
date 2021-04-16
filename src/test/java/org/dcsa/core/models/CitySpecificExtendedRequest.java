package org.dcsa.core.models;

import org.dcsa.core.extendedrequest.*;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.Join;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.r2dbc.core.binding.BindMarkers;

public class CitySpecificExtendedRequest extends ExtendedRequest<City> {
    public CitySpecificExtendedRequest(ExtendedParameters extendedParameters, R2dbcDialect r2dbcDialect) {
        super(extendedParameters, r2dbcDialect, City.class);
    }

    protected DBEntityAnalysis.DBEntityAnalysisBuilder<City> prepareDBEntityAnalysis() {
        DBEntityAnalysis.DBEntityAnalysisBuilder<City> builder = super.prepareDBEntityAnalysis();
        Table cityTable = builder.getPrimaryModelTable();
        Table countryTable = Table.create(SqlIdentifier.unquoted(ReflectUtility.getTableName(County.class))).as(SqlIdentifier.unquoted("c"));

        return builder
                .join(Join.JoinType.JOIN, cityTable, countryTable, County.class)
                .onFieldEqualsThen("countryId", "id")
                .registerQueryField(SqlIdentifier.unquoted("country_name"), "cn", String.class);
    }
}
