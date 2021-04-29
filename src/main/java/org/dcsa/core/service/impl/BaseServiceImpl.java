package org.dcsa.core.service.impl;

import org.dcsa.core.service.BaseService;
import org.dcsa.core.util.ReflectUtility;

public abstract class BaseServiceImpl<T, I> implements BaseService<T, I> {

    private transient Class<T> modelClass;

    protected String getType() {
        return getModelClass().getSimpleName();
    }

    public Class<T> getModelClass() {
        if (modelClass == null) {
            this.modelClass = ReflectUtility.getConcreteModelClassForService(this.getClass());
        }
        return modelClass;
    }
}
