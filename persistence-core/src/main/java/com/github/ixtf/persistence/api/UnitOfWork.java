package com.github.ixtf.persistence.api;

import com.github.ixtf.persistence.IEntity;

/**
 * @author jzb 2019-02-18
 */
public interface UnitOfWork {

    UnitOfWork registerNew(IEntity o);

    UnitOfWork registerDirty(IEntity o);

    UnitOfWork registerClean(IEntity o);

    UnitOfWork registerDelete(IEntity o);

    UnitOfWork registerSave(IEntity o);

    UnitOfWork commit();

    UnitOfWork rollback();
}
