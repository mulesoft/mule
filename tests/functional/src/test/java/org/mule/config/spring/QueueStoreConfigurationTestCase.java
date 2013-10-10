/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.QueueStore;
import org.mule.config.QueueProfile;
import org.mule.construct.Flow;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.model.seda.SedaService;
import org.mule.processor.strategy.QueuedAsynchronousProcessingStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;

import org.junit.Test;

public class QueueStoreConfigurationTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/spring/queue-store-configs.xml";
    }

    @Test
    public void testServiceDefaults()
    {
        SedaService service = lookupService("serviceDefault");
        QueueProfile queueProfile = service.getQueueProfile();
        assertEquals(0, queueProfile.getMaxOutstandingMessages());
        assertObjectStoreIsDefaultMemoryObjectStore(queueProfile.getObjectStore());
    }
    
    @Test
    public void testServiceOnlyNumberOfOutstandingMessagesConfigured()
    {
        SedaService service = lookupService("serviceNoObjectStore");
        QueueProfile queueProfile = service.getQueueProfile();
        assertEquals(42, queueProfile.getMaxOutstandingMessages());
        assertObjectStoreIsDefaultMemoryObjectStore(queueProfile.getObjectStore());
    }
    
    @Test
    public void testServiceExplicitDefaultMemoryObjectStoreConfigured()
    {
        SedaService service = lookupService("serviceExplicitDefaultMemoryObjectStore");
        QueueProfile queueProfile = service.getQueueProfile();
        assertObjectStoreIsDefaultMemoryObjectStore(queueProfile.getObjectStore());
    }
    
    @Test
    public void testServiceExplicitDefaultPersistentObjectStoreConfigured()
    {
        SedaService service = lookupService("serviceExplicitDefaultPersistentObjectStore");
        QueueProfile queueProfile = service.getQueueProfile();
        assertObjectStoreIsDefaultPersistentObjectStore(queueProfile.getObjectStore());
    }

    @Test
    public void testServiceExplicitObjectStoreConfigured()
    {
        SedaService service = lookupService("serviceExplicitObjectStore");
        QueueProfile queueProfile = service.getQueueProfile();
        assertTrue(queueProfile.getObjectStore() instanceof TestQueueStore);
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

    private SedaService lookupService(String name)
    {
        return (SedaService) muleContext.getRegistry().lookupService(name);
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
