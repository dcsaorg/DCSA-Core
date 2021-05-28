package org.dcsa.core.stub;

import io.r2dbc.spi.ColumnMetadata;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class StubColumnMetadata implements ColumnMetadata {
    private final String name;
    private final Class<?> javaType;
}
