package org.dcsa.core.stub;

import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Row;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor(staticName = "of")
public class StubRow implements Row {
    private final Map<String, ?> name2Object;
    private final List<ColumnMetadata> columnMetadata;

    @Override
    public <T> T get(int i, Class<T> clazz) {
        throw new UnsupportedOperationException("stub; not implemented");
    }

    @Override
    public <T> T get(String columnName, Class<T> clazz) {
        return clazz.cast(name2Object.get(columnName));
    }

    public List<ColumnMetadata> getStubColumnMetadatas() {
        if (columnMetadata == null) {
          throw new IllegalStateException("You must have an explicit column metadata definition when the row"
            + " contain null values;");
        }
        return columnMetadata;
    }

    public static StubRow of(Map<String, ?> name2Object) {
      if (name2Object.values().stream().anyMatch(Objects::isNull)) {
        return of(name2Object, (List<ColumnMetadata>) null);
      }
      return of(name2Object,
        name2Object.keySet().stream()
          .map(column -> (ColumnMetadata) StubColumnMetadata.of(column, name2Object.get(column).getClass()))
          .collect(Collectors.toList())
      );
    }

  public static StubRow of(Map<String, ?> name2Object, Map<String, Class<?>> columnTypes) {
    return of(name2Object,
      name2Object.keySet().stream()
        .<ColumnMetadata>map(column -> {
          Class<?> clazz = columnTypes.get(column);
          if (clazz == null) {
            Object value = name2Object.get(column);
            if (value == null) {
              throw new IllegalArgumentException("Column must be defined in columnTypes when its value is null!"
                + " (column: " + column + ")");
            }
            clazz = value.getClass();
          }
          return StubColumnMetadata.of(column, clazz);
        })
        .collect(Collectors.toList())
    );
  }
}
