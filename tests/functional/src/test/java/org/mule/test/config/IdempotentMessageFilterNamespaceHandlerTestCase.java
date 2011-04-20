/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.store.ObjectStore;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.routing.IdempotentMessageFilter;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.SystemUtils;
import org.mule.util.store.InMemoryObjectStore;
import org.mule.util.store.SimpleMemoryObjectStore;
import org.mule.util.store.TextFileObjectStore;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * Tests for all object stores that can be configured on an {@link IdempotentMessageFilter}.
 */
public class IdempotentMessageFilterNamespaceHandlerTestCase extends FunctionalTestCase
{
    public IdempotentMessageFilterNamespaceHandlerTestCase()
    {
        // we just test the wiring of the objects, no need to start the MuleContext
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/idempotent-message-filter-config.xml";
    }

    public void testInMemoryObjectStore() throws Exception
    {
        IdempotentMessageFilter filter = idempotentMessageFilterFromFlow("inMemoryStore");

        ObjectStore<?> store = filter.getStore();
        assertEquals(InMemoryObjectStore.class, store.getClass());

        InMemoryObjectStore<?> memoryStore = (InMemoryObjectStore<?>) store;
        assertEquals(1000, memoryStore.getEntryTTL());
        assertEquals(2000, memoryStore.getExpirationInterval());
        assertEquals(3000, memoryStore.getMaxEntries());
    }

    public void testSimpleTextFileStore() throws Exception
    {
        IdempotentMessageFilter filter = idempotentMessageFilterFromFlow("simpleTextFileStore");

        ObjectStore<?> store = filter.getStore();
        assertEquals(TextFileObjectStore.class, store.getClass());

        TextFileObjectStore fileStore = (TextFileObjectStore) store;
        assertEquals("the-store", fileStore.getName());

        File tmpDir = SystemUtils.getJavaIoTmpDir();
        assertEquals(tmpDir.getAbsolutePath(), fileStore.getDirectory());

        assertEquals(1000, fileStore.getEntryTTL());
        assertEquals(2000, fileStore.getExpirationInterval());
        assertEquals(3000, fileStore.getMaxEntries());
    }

    public void testCustomObjectStore() throws Exception
    {
        IdempotentMessageFilter filter = idempotentMessageFilterFromFlow("customObjectStore");

        ObjectStore<?> store = filter.getStore();
        assertEquals(CustomObjectStore.class, store.getClass());

        CustomObjectStore customStore = (CustomObjectStore) store;
        assertEquals("the-value", customStore.getCustomProperty());
    }

    private IdempotentMessageFilter idempotentMessageFilterFromFlow(String flowName) throws Exception
    {
        FlowConstruct flow = getFlowConstruct(flowName);
        assertTrue(flow instanceof SimpleFlowConstruct);

        SimpleFlowConstruct simpleFlow = (SimpleFlowConstruct) flow;
        List<MessageProcessor> processors = simpleFlow.getMessageProcessors();
        assertEquals(1, processors.size());

        MessageProcessor firstMP = processors.get(0);
        assertEquals(IdempotentMessageFilter.class, firstMP.getClass());

        return (IdempotentMessageFilter) firstMP;
    }

    public static class CustomObjectStore extends SimpleMemoryObjectStore<Serializable>
    {
        private String customProperty;

        public String getCustomProperty()
        {
            return customProperty;
        }

        public void setCustomProperty(String value)
        {
            customProperty = value;
        }
    }
}
