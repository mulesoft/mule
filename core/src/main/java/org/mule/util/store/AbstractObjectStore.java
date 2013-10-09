/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is an abstract superclass for {@link ObjectStore} implementations that conforms to the
 * contract defined in the interface's javadocs. Subclasses only need to implement storing the
 * actual objects.
 */
public abstract class AbstractObjectStore<T extends Serializable> implements ObjectStore<T>
{
    protected final Log logger = LogFactory.getLog(getClass());

    public boolean contains(Serializable key) throws ObjectStoreException
    {
        if (key == null)
        {
            throw new ObjectStoreException(CoreMessages.objectIsNull("key"));
        }
        return doContains(key);
    }

    protected abstract boolean doContains(Serializable key) throws ObjectStoreException;

    public void store(Serializable key, T value) throws ObjectStoreException
    {
        if (key == null)
        {
            throw new ObjectStoreException(CoreMessages.objectIsNull("key"));
        }

        if (contains(key))
        {
            throw new ObjectAlreadyExistsException();
        }

        doStore(key, value);
    }

    protected  abstract void doStore(Serializable key, T value) throws ObjectStoreException;

    public T retrieve(Serializable key) throws ObjectStoreException
    {
        if (key == null)
        {
            throw new ObjectStoreException(CoreMessages.objectIsNull("key"));
        }

        if (contains(key) == false)
        {
            String message = "Key does not exist: " + key;
            throw new ObjectDoesNotExistException(CoreMessages.createStaticMessage(message));
        }

        return doRetrieve(key);
    }

    protected abstract T doRetrieve(Serializable key) throws ObjectStoreException;

    public T remove(Serializable key) throws ObjectStoreException
    {
        if (key == null)
        {
            throw new ObjectStoreException(CoreMessages.objectIsNull("key"));
        }

        if (contains(key) == false)
        {
            throw new ObjectDoesNotExistException();
        }

        return doRemove(key);
    }

    protected abstract T doRemove(Serializable key) throws ObjectStoreException;
}
