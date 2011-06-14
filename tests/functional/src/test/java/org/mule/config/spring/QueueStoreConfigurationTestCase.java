/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.config.MuleProperties;
import org.mule.api.construct.PipelineProcessingStrategy;
import org.mule.api.store.ListableObjectStore;
import org.mule.config.QueueProfile;
import org.mule.construct.Flow;
import org.mule.construct.QueuedAsynchronousProcessingStrategy;
import org.mule.construct.SynchronousProcessingStrategy;
import org.mule.model.seda.SedaService;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;

public class QueueStoreConfigurationTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/spring/queue-store-configs.xml";
    }

    public void testServiceDefaults()
    {
        SedaService service = lookupService("serviceDefault");
        QueueProfile queueProfile = service.getQueueProfile();
        assertEquals(0, queueProfile.getMaxOutstandingMessages());
        assertObjectStoreIsDefaultMemoryObjectStore(queueProfile.getObjectStore());
    }
    
    public void testServiceOnlyNumberOfOutstandingMessagesConfigured()
    {
        SedaService service = lookupService("serviceNoObjectStore");
        QueueProfile queueProfile = service.getQueueProfile();
        assertEquals(42, queueProfile.getMaxOutstandingMessages());
        assertObjectStoreIsDefaultMemoryObjectStore(queueProfile.getObjectStore());
    }
    
    public void testServiceExplicitDefaultMemoryObjectStoreConfigured()
    {
        SedaService service = lookupService("serviceExplicitDefaultMemoryObjectStore");
        QueueProfile queueProfile = service.getQueueProfile();
        assertObjectStoreIsDefaultMemoryObjectStore(queueProfile.getObjectStore());
    }
    
    public void testServiceExplicitDefaultPersistentObjectStoreConfigured()
    {
        SedaService service = lookupService("serviceExplicitDefaultPersistentObjectStore");
        QueueProfile queueProfile = service.getQueueProfile();
        assertObjectStoreIsDefaultPersistentObjectStore(queueProfile.getObjectStore());
    }

    public void testServiceExplicitObjectStoreConfigured()
    {
        SedaService service = lookupService("serviceExplicitObjectStore");
        QueueProfile queueProfile = service.getQueueProfile();
        assertTrue(queueProfile.getObjectStore() instanceof TestObjectStore);
    }

    public void testFlowDefaults()
    {
        Flow flow = lookupFlow("flowDefault");
        
        // default for flow is sync processing -> no queueing
        assertTrue(flow.getProcessingStrategy() instanceof SynchronousProcessingStrategy);
    }
    
    public void testFlowQueuedAsync()
    {
        Flow flow = lookupFlow("flowQueuedAsync");
        
        
        PipelineProcessingStrategy pipeline = flow.getProcessingStrategy();
        assertTrue(pipeline instanceof QueuedAsynchronousProcessingStrategy);
        
        QueuedAsynchronousProcessingStrategy queuedPipeline = (QueuedAsynchronousProcessingStrategy)pipeline;
        assertObjectStoreIsDefaultMemoryObjectStore(queuedPipeline.getQueueStore());
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
            muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME);
        assertEquals(defaultMemoryObjectStore, objectStore);
    }
    
    private void assertObjectStoreIsDefaultPersistentObjectStore(ListableObjectStore<Serializable> objectStore)
    {
        Object defaultPersistentObjectStore = 
            muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_STORE_DEFAULT_PERSISTENT_NAME);
        assertEquals(defaultPersistentObjectStore, objectStore);
    }
    
    public static class TestObjectStore extends SimpleMemoryObjectStore<Serializable>
    {
        // no custom methods
    }
}
