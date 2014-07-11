/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.store.PartitionableObjectStore;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.size.SmallTest;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class MuleObjectStoreManagerTestCase extends AbstractMuleTestCase
{

    private MuleObjectStoreManager storeManager = new MuleObjectStoreManager();
    private File tempWorkDir;

    @Test
    public void disposeDisposableStore() throws ObjectStoreException
    {
        @SuppressWarnings("unchecked")
        ObjectStore<Serializable> store = Mockito.mock(ObjectStore.class, Mockito.withSettings()
            .extraInterfaces(Disposable.class));

        this.storeManager.disposeStore(store);

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

    @Test
    public void ensureTransientPartitionIsCleared() throws ObjectStoreException, InitialisationException
    {
        ensurePartitionIsCleared(false);
    }

    @Test
    public void ensurePersistentPartitionIsCleared() throws ObjectStoreException, InitialisationException
    {
        ensurePartitionIsCleared(true);
    }

    private void ensurePartitionIsCleared(boolean isPersistent) throws ObjectStoreException, InitialisationException
    {
        String partitionName = "partition";
        try
        {
            ObjectStorePartition<Serializable> store = createStorePartition(partitionName, isPersistent);

            store.getBaseStore().store("Some Key", "Some Value", partitionName);

            assertEquals(1, store.allKeys().size());

            storeManager.disposeStore(store);

            assertEquals(0, store.allKeys().size());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            storeManager.dispose();
        }
    }

    @Test
    public void removeStoreAndMonitorOnTransientPartition() throws ObjectStoreException, InitialisationException
    {
        removeStoreAndMonitor(false);
    }

    @Test
    public void removeStoreAndMonitorOnPersistentPartition() throws ObjectStoreException, InitialisationException
    {
        removeStoreAndMonitor(true);
    }

    private void removeStoreAndMonitor(boolean isPersistent) throws ObjectStoreException, InitialisationException
    {
        String partitionName = "partition";
        try
        {
            ObjectStorePartition<Serializable> store = createStorePartition(partitionName, isPersistent);

            assertEquals(1, storeManager.scheduler.getActiveCount());

            storeManager.disposeStore(store);

            assertFalse(storeManager.stores.containsKey(partitionName));

            new PollingProber(1000, 60).check(new Probe()
            {
                @Override
                public boolean isSatisfied()
                {
                    return storeManager.scheduler.getActiveCount() == 0;
                }

                @Override
                public String describeFailure()
                {
                    return "There are active scheduler tasks.";
                }
            });
        }
        finally
        {
            storeManager.dispose();
        }
    }

    @After
    public void deleteTempWorkDir() throws IOException
    {
        if(tempWorkDir!=null && tempWorkDir.exists())
        {
            FileUtils.deleteDirectory(tempWorkDir);
        }
    }

    private ObjectStorePartition<Serializable> createStorePartition(String partitionName, boolean isPersistent) throws InitialisationException
    {
        MuleContext muleContext = Mockito.mock(MuleContext.class);

        createRegistryAndBaseStore(muleContext, isPersistent);

        storeManager.setMuleContext(muleContext);
        storeManager.initialise();

        ObjectStorePartition<Serializable> store = storeManager
                .getObjectStore(partitionName, isPersistent, ObjectStoreManager.UNBOUNDED, 10000, 50);

        assertTrue(storeManager.stores.containsKey(partitionName));

        return store;
    }

    private void createRegistryAndBaseStore(MuleContext muleContext, boolean isPersistent)
    {
        MuleRegistry muleRegistry = Mockito.mock(MuleRegistry.class);
        if (isPersistent)
        {
            PartitionableObjectStore<?> store = createPersistentPartitionableObjectStore(muleContext);
            Mockito.when(muleRegistry.lookupObject(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME))
                    .thenReturn(store);
        }
        else
        {
            PartitionableObjectStore<?> store = createTransientPartitionableObjectStore();
            Mockito.when(muleRegistry.lookupObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME))
                    .thenReturn(store);
        }

        Mockito.when(muleContext.getRegistry()).thenReturn(muleRegistry);
    }

    private PartitionableObjectStore<?> createTransientPartitionableObjectStore()
    {
        return new PartitionedInMemoryObjectStore<>();
    }

    private PartitionableObjectStore<?> createPersistentPartitionableObjectStore(MuleContext muleContext)
    {
        MuleConfiguration muleConfiguration = Mockito.mock(MuleConfiguration.class);
        tempWorkDir = new File("TempWorkDir" + System.currentTimeMillis());
        Mockito.when(muleConfiguration.getWorkingDirectory()).thenReturn(tempWorkDir.getAbsolutePath());
        Mockito.when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

        return new PartitionedPersistentObjectStore<>(muleContext);
    }

    @Test
    public void dontFailIfUnsupported() throws ObjectStoreException
    {
        @SuppressWarnings("unchecked")
        ObjectStore<Serializable> store = Mockito.mock(ObjectStore.class, Mockito.withSettings()
            .extraInterfaces(Disposable.class));

        Mockito.doThrow(UnsupportedOperationException.class).when(store).clear();

        storeManager.disposeStore(store);

        Mockito.verify(store).clear();
        Mockito.verify((Disposable) store).dispose();
    }

}
