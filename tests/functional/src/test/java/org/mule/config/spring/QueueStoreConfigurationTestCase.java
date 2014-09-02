/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.QueueStore;
import org.mule.construct.Flow;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.processor.strategy.QueuedAsynchronousProcessingStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;

import org.junit.Test;

public class QueueStoreConfigurationTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/spring/queue-store-configs.xml";
    }

    @Test
    public void testServiceDefaults()
    {
        Flow service = lookupFlow("serviceDefault");
        QueuedAsynchronousProcessingStrategy ps = (QueuedAsynchronousProcessingStrategy) service.getProcessingStrategy();
        assertEquals(0, ps.getMaxQueueSize().intValue());
        assertObjectStoreIsDefaultMemoryObjectStore(ps.getQueueStore());
    }
    
    @Test
    public void testServiceOnlyNumberOfOutstandingMessagesConfigured()
    {
        Flow service = lookupFlow("serviceNoObjectStore");
        QueuedAsynchronousProcessingStrategy ps = (QueuedAsynchronousProcessingStrategy) service.getProcessingStrategy();
        assertEquals(42, ps.getMaxQueueSize().intValue());
        assertObjectStoreIsDefaultMemoryObjectStore(ps.getQueueStore());
    }
    
    @Test
    public void testServiceExplicitDefaultMemoryObjectStoreConfigured()
    {
        Flow service = lookupFlow("serviceExplicitDefaultMemoryObjectStore");
        QueuedAsynchronousProcessingStrategy ps = (QueuedAsynchronousProcessingStrategy) service.getProcessingStrategy();
        assertObjectStoreIsDefaultMemoryObjectStore(ps.getQueueStore());
    }
    
    @Test
    public void testServiceExplicitDefaultPersistentObjectStoreConfigured()
    {
        Flow service = lookupFlow("serviceExplicitDefaultPersistentObjectStore");
        QueuedAsynchronousProcessingStrategy ps = (QueuedAsynchronousProcessingStrategy) service.getProcessingStrategy();
        assertObjectStoreIsDefaultPersistentObjectStore(ps.getQueueStore());
    }

    @Test
    public void testServiceExplicitObjectStoreConfigured()
    {
        Flow service = lookupFlow("serviceExplicitObjectStore");
        QueuedAsynchronousProcessingStrategy ps = (QueuedAsynchronousProcessingStrategy) service.getProcessingStrategy();
        assertTrue(ps.getQueueStore() instanceof TestQueueStore);
    }

    @Test
    public void testFlowDefaults()
    {
        Flow flow = lookupFlow("flowDefault");
        
        // default for flow is sync processing -> no queueing
        assertTrue(flow.getProcessingStrategy() instanceof DefaultFlowProcessingStrategy);
    }
    
    @Test
    public void testFlowQueuedAsync()
    {
        Flow flow = lookupFlow("flowQueuedAsync");

        ProcessingStrategy pipeline = flow.getProcessingStrategy();
        assertTrue(pipeline instanceof QueuedAsynchronousProcessingStrategy);
        
        QueuedAsynchronousProcessingStrategy queuedPipeline = (QueuedAsynchronousProcessingStrategy)pipeline;
        assertObjectStoreIsDefaultMemoryObjectStore(queuedPipeline.getQueueStore());
    }

    @Test
    public void testFlowQueuedAsyncWithPersistentObjectStore()
    {
        Flow flow = lookupFlow("flowQueuedAsyncPersistentStore");

        ProcessingStrategy pipeline = flow.getProcessingStrategy();
        assertTrue(pipeline instanceof QueuedAsynchronousProcessingStrategy);

        QueuedAsynchronousProcessingStrategy queuedPipeline = (QueuedAsynchronousProcessingStrategy) pipeline;
        assertObjectStoreIsDefaultPersistentObjectStore(queuedPipeline.getQueueStore());
    }

    private Flow lookupFlow(String name)
    {
        return (Flow) muleContext.getRegistry().lookupFlowConstruct(name);
    }

    private void assertObjectStoreIsDefaultMemoryObjectStore(ListableObjectStore<Serializable> objectStore)
    {
        Object defaultMemoryObjectStore =
            muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
        assertEquals(defaultMemoryObjectStore, objectStore);
    }
    
    private void assertObjectStoreIsDefaultPersistentObjectStore(ListableObjectStore<Serializable> objectStore)
    {
        Object defaultPersistentObjectStore =
            muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_PERSISTENT_NAME);
        assertEquals(defaultPersistentObjectStore, objectStore);
    }
    
    public static class TestQueueStore extends SimpleMemoryObjectStore<Serializable> implements QueueStore<Serializable>
    {
        // no custom methods
    }
}
