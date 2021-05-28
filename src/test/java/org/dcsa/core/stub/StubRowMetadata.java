package org.dcsa.core.stub;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.RowMetadata;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class StubRowMetadata implements RowMetadata {
    private final Collection<ColumnMetadata> columnMetadatas;

    @Override
    public ColumnMetadata getColumnMetadata(int i) {
        throw new UnsupportedOperationException("stub; not implemented");
    }

    @Override
    public ColumnMetadata getColumnMetadata(String columnName) {
        return columnMetadatas.stream().filter(columnMetadata -> columnMetadata.getName().equals(columnName))
                .findFirst().orElse(null);
    }

    @Override
    public Collection<String> getColumnNames() {
        return columnMetadatas.stream().map(ColumnMetadata::getName).collect(Collectors.toSet());
    }
}
