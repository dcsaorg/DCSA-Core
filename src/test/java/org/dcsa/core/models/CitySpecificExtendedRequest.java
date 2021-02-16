package org.dcsa.core.models;

import org.dcsa.core.extendedrequest.*;
import org.springframework.data.relational.core.sql.Join;

public class CitySpecificExtendedRequest extends ExtendedRequest<City> {
    public CitySpecificExtendedRequest(ExtendedParameters extendedParameters) {
        super(extendedParameters, City.class);
    }

    public Class<?> getPrimaryModelClass() {
        return this.getModelClass();
    }

    protected void findAllTablesAndBuildJoins() {
        super.findAllTablesAndBuildJoins();
        JoinDescriptor joinDescriptor = SimpleJoinDescriptor.of(
                Join.JoinType.JOIN,
                getTableName(County.class),
                "c",
                "ON c.id = " + getTableName(getModelClass()) + ".country_id",
                null
        );
        registerJoinDescriptor(joinDescriptor);
    }

    protected void loadFieldsFromSubclass() {
        registerQueryField(QueryFields.nonSelectableQueryField(
                "c", "country_name", "cn", String.class
        ));
    }
}
