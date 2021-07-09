package org.dcsa.core.query.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;

@Data
@RequiredArgsConstructor(staticName = "of")
public class QField {
    private final Field field;
}
