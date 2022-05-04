package org.dcsa.core.extendedrequest.testsupport;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.BindTarget;

@RequiredArgsConstructor(staticName = "of")
class MockBindMarker implements BindMarker {

  @Getter
  private final String placeholder;

  @Override
  public void bind(BindTarget bindTarget, Object value) {
    // Do nothing
  }

  @Override
  public void bindNull(BindTarget bindTarget, Class<?> valueType) {
    // Do nothing
  }
}
