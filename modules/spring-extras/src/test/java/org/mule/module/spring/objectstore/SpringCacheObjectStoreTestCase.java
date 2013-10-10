/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.objectstore;

import org.mule.api.store.ObjectStore;
import org.mule.transport.NullPayload;
import org.mule.util.store.AbstractObjectStoreContractTestCase;

import java.io.Serializable;

import org.mockito.Mockito;
import org.springmodules.cache.CachingModel;
import org.springmodules.cache.provider.CacheProviderFacade;

import static org.mockito.Mockito.mock;

public class SpringCacheObjectStoreTestCase extends AbstractObjectStoreContractTestCase
{

    @Override
    public ObjectStore<Serializable> getObjectStore()
    {
        SpringCacheObjectStore<Serializable> objectStore = new SpringCacheObjectStore<Serializable>();
        CachingModel cachingModel = mock(CachingModel.class);
        CacheProviderFacade cacheProvider = mock(CacheProviderFacade.class);
        objectStore.setCacheProvider(cacheProvider);
        objectStore.setCachingModel(cachingModel);

        Mockito.when(cacheProvider.getFromCache("this_key_does_not_exist", cachingModel)).thenReturn(null);
        Mockito.when(cacheProvider.getFromCache("theKey", cachingModel)).thenReturn(null).thenReturn(new SpringCacheObjectStore.ObjectStoreValue<Serializable>(null));

        return objectStore;
    }

    @Override
    public Serializable getStorableValue()
    {
        return NullPayload.getInstance();
    }
}
