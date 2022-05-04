package org.dcsa.core.extendedrequest.testsupport;

import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

@FunctionalInterface
public interface TriFunction<A, B, C, D> {
  D apply(A a, B b, C c);
}
