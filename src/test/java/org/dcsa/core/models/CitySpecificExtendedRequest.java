package org.dcsa.core.models;

import org.dcsa.core.extendedrequest.*;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.sql.Join;

public class CitySpecificExtendedRequest extends ExtendedRequest<City> {
    public CitySpecificExtendedRequest(ExtendedParameters extendedParameters) {
        super(extendedParameters, City.class);
    }

    protected DBEntityAnalysis.DBEntityAnalysisBuilder<City> prepareDBEntityAnalysis() {
        JoinDescriptor joinDescriptor = SimpleJoinDescriptor.of(
                Join.JoinType.JOIN,
                ReflectUtility.getTableName(County.class),
                "c",
                "ON c.id = " + ReflectUtility.getTableName(getModelClass()) + ".country_id",
                null
        );
        return super.prepareDBEntityAnalysis()
                .registerJoinDescriptor(joinDescriptor)
                .registerQueryField(QueryFields.nonSelectableQueryField(
                        "c", "country_name", "cn", String.class
                ));
    }
}
