package org.dcsa.core.stub;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Row;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor(staticName = "of")
public class StubRow implements Row {
    private final Map<String, ?> name2Object;

    @Override
    public <T> T get(int i, Class<T> clazz) {
        throw new UnsupportedOperationException("stub; not implemented");
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T get(String columnName, Class<T> clazz) {
        return (T) name2Object.get(columnName);
    }

    public List<ColumnMetadata> getStubColumnMetadatas() {
        return name2Object.keySet().stream()
                .map(column -> (ColumnMetadata) StubColumnMetadata.of(column, name2Object.get(column).getClass()))
                .collect(Collectors.toList());
    }
}
