package org.dcsa.core.service.impl;

import org.dcsa.core.repository.ExtendedRepository;
import org.dcsa.core.util.ReflectUtility;

// Implementation detail of the (Asymmetric)QueryService
abstract class QueryServiceImplSupport<R extends ExtendedRepository<DM, I>, DM, I> {

    /**
     * Getter for the repository instance
     */
    protected abstract R getRepository();

    private transient Class<DM> modelClass;


    // Documented via the interfaces (that subclasses implement)
    public Class<DM> getModelClass() {
        if (modelClass == null) {
            this.modelClass = ReflectUtility.getConcreteModelClassForService(this.getClass());
        }
        return modelClass;
    }


}
