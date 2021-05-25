package org.dcsa.core.stub;

import io.r2dbc.spi.ColumnMetadata;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor()
public class StubColumnMetadata implements ColumnMetadata {
    private final String name;
    private final Class<?> clazz;

    @Override
    public Class<?> getJavaType() {
        return clazz;
    }

    @Override
    public String getName() {
        return name;
    }
}
