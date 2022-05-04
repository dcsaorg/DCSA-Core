package org.dcsa.core.extendedrequest.testsupport;

import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.BindMarkers;

class MockBindMarkers implements BindMarkers {

  private int counter = 1;

  @Override
  public BindMarker next() {
    return MockBindMarker.of("$" + counter++);
  }

  @Override
  public BindMarker next(String hint) {
    return MockBindMarker.of(":" + hint);
  }
}
