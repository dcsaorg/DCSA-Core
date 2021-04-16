package org.dcsa.core.mock;

import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.BindMarkers;

public class MockBindMarkers implements BindMarkers {

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
