/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;

import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class MuleObjectStoreManagerTestCase extends AbstractMuleTestCase
{

    private MuleObjectStoreManager storeManager = new MuleObjectStoreManager();

    @Test
    public void disposeDisposableStore() throws ObjectStoreException
    {
        @SuppressWarnings("unchecked")
        ObjectStore<Serializable> store = Mockito.mock(ObjectStore.class, Mockito.withSettings()
            .extraInterfaces(Disposable.class));

        storeManager.disposeStore(store);

        Mockito.verify(store).clear();
        Mockito.verify((Disposable) store).dispose();
    }

    @Test
    public void disposePartitionableStore() throws ObjectStoreException
    {
        String partitionName = "partition";
        
        @SuppressWarnings("unchecked")
        ObjectStorePartition<Serializable> store = Mockito.mock(ObjectStorePartition.class,
            Mockito.withSettings()
                .extraInterfaces(Disposable.class)
                .defaultAnswer(Mockito.RETURNS_DEEP_STUBS));
        
        Mockito.when(store.getPartitionName()).thenReturn(partitionName);

        storeManager.disposeStore(store);
        
        Mockito.verify(store.getBaseStore()).disposePartition(partitionName);
        Mockito.verify(store, Mockito.never()).clear();
        Mockito.verify((Disposable) store).dispose();
    }
}
