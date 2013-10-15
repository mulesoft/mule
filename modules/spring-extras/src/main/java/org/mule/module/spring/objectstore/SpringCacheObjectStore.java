/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.objectstore;

import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;

import java.io.Serializable;

import org.springmodules.cache.CachingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

/**
 * Implements an {@link ObjectStore} using Spring module cache.
 */
public class SpringCacheObjectStore<T extends Serializable> implements ObjectStore<T>
{

    private CacheProviderFacade cacheProvider;
    private CachingModel cachingModel;

    public boolean contains(Serializable key) throws ObjectStoreException
    {
        synchronized (cacheProvider)
        {
            assertKeyNotNull(key);

            try
            {
                return cacheProvider.getFromCache(key, cachingModel) != null;
            }
            catch (Exception e)
            {
                throw new ObjectStoreException(e);
            }
        }
    }

    public void store(Serializable key, T value) throws ObjectStoreException
    {
        synchronized (cacheProvider)
        {
            if (contains(key))
            {
                throw new ObjectAlreadyExistsException();
            }

            ObjectStoreValue<T> wrappedValue = new ObjectStoreValue<T>(value);
            cacheProvider.putInCache(key, cachingModel, wrappedValue);
        }
    }

    public T retrieve(Serializable key) throws ObjectStoreException
    {
        assertKeyNotNull(key);

        synchronized (cacheProvider)
        {
            ObjectStoreValue cachedValue = (ObjectStoreValue) cacheProvider.getFromCache(key, cachingModel);

            if (cachedValue == null)
            {
                throw new ObjectDoesNotExistException(CoreMessages.objectNotFound(key));
            }
            else
            {
                return (T) cachedValue.getValue();
            }
        }
    }

    public T remove(Serializable key) throws ObjectStoreException
    {
        synchronized (cacheProvider)
        {
            if (contains(key))
            {
                ObjectStoreValue objectStoreValue = (ObjectStoreValue) cacheProvider.getFromCache(key, cachingModel);
                cacheProvider.removeFromCache(key, cachingModel);

                return (T) objectStoreValue.getValue();
            }
            else
            {
                throw new ObjectDoesNotExistException(CoreMessages.objectNotFound(key));
            }
        }
    }

    public boolean isPersistent()
    {
        return true;
    }

    public CacheProviderFacade getCacheProvider()
    {
        return cacheProvider;
    }

    public void setCacheProvider(CacheProviderFacade cacheProvider)
    {
        this.cacheProvider = cacheProvider;
    }

    public CachingModel getCachingModel()
    {
        return cachingModel;
    }

    public void setCachingModel(CachingModel cachingModel)
    {
        this.cachingModel = cachingModel;
    }

    private void assertKeyNotNull(Serializable key) throws ObjectStoreException
    {
        if (key == null)
        {
            throw new ObjectStoreException(CoreMessages.objectIsNull("id"));
        }
    }

    /**
     * Provides a place holder to store values inside a Spring cache to be able
     * to save null values. This is required because the base API does not have
     * a method to detect whether a given key is present in the cache or not.
     *
     * @param <T> the type of elements stored in the cache.
     */
    public static class ObjectStoreValue<T extends Serializable> implements Serializable
    {

        private final T value;

        public ObjectStoreValue(T value)
        {
            this.value = value;
        }

        public T getValue()
        {
            return value;
        }
    }
}
