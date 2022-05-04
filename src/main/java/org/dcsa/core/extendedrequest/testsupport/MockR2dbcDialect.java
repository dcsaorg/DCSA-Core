package org.dcsa.core.extendedrequest.testsupport;

import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;

/**
 * Implementation detail of the test support framework.
 *
 * Public only because of a test (RowMapperTest) needs it.
 */
public class MockR2dbcDialect extends PostgresDialect {

  @Override
  public BindMarkersFactory getBindMarkersFactory() {
    return MockBindMarkers::new;
  }
}
