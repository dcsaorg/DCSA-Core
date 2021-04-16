package org.dcsa.core.mock;

import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;

public class MockR2dbcDialect extends PostgresDialect {

    @Override
    public BindMarkersFactory getBindMarkersFactory() {
        return MockBindMarkers::new;
    }
}
