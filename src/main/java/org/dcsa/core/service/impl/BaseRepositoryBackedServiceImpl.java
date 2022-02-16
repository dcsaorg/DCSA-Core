package org.dcsa.core.service.impl;

import org.dcsa.core.repository.ExtendedRepository;
import org.dcsa.core.util.ReflectUtility;

public abstract class BaseRepositoryBackedServiceImpl<R extends ExtendedRepository<T, I>, T, I> {

    protected abstract R getRepository();

    private transient Class<T> modelClass;

    /**
     * TODO: Weaken to protected if possible
     */
    public Class<T> getModelClass() {
        if (modelClass == null) {
            this.modelClass = ReflectUtility.getConcreteModelClassForService(this.getClass());
        }
        return modelClass;
    }
}
