/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.IdempotentMessageFilter;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;
import org.mule.runtime.core.util.SystemUtils;
import org.mule.runtime.core.util.store.InMemoryObjectStore;
import org.mule.runtime.core.util.store.SimpleMemoryObjectStore;
import org.mule.runtime.core.util.store.TextFileObjectStore;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import org.junit.Test;

/**
 * Tests for all object stores that can be configured on an {@link IdempotentMessageFilter}.
 */
public class IdempotentMessageFilterNamespaceHandlerTestCase extends AbstractIntegrationTestCase
{
    public IdempotentMessageFilterNamespaceHandlerTestCase()
    {
        // we just test the wiring of the objects, no need to start the MuleContext
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/config/idempotent-message-filter-config.xml";
    }

    @Test
    public void testInMemoryObjectStore() throws Exception
    {
        final IdempotentMessageFilter filter = idempotentMessageFilterFromFlow("inMemoryStore");

        final ObjectStore<?> store = filter.getStore();
        assertEquals(InMemoryObjectStore.class, store.getClass());

        final InMemoryObjectStore<?> memoryStore = (InMemoryObjectStore<?>) store;
        assertEquals(1000, memoryStore.getEntryTTL());
        assertEquals(2000, memoryStore.getExpirationInterval());
        assertEquals(3000, memoryStore.getMaxEntries());
    }

    @Test
    public void testSimpleTextFileStore() throws Exception
    {
        final IdempotentMessageFilter filter = idempotentMessageFilterFromFlow("simpleTextFileStore");

        final ObjectStore<?> store = filter.getStore();
        assertEquals(TextFileObjectStore.class, store.getClass());

        final TextFileObjectStore fileStore = (TextFileObjectStore) store;
        final File tmpDir = SystemUtils.getJavaIoTmpDir();
        assertEquals(tmpDir.getCanonicalPath(), new File(fileStore.getDirectory()).getCanonicalPath());

        assertEquals(1000, fileStore.getEntryTTL());
        assertEquals(2000, fileStore.getExpirationInterval());
        assertEquals(3000, fileStore.getMaxEntries());
    }

    @Test
    public void testCustomObjectStore() throws Exception
    {
        testPojoObjectStore("customObjectStore");
    }
    
    @Test
    public void testOnUnaccepted() throws Exception
    {
        final IdempotentMessageFilter filter = idempotentMessageFilterFromFlow("idempotentFilterWithOnUnacceptedMP");
        assertNotNull(filter.getUnacceptedMessageProcessor());
        assertEquals(StringAppendTransformer.class, filter.getUnacceptedMessageProcessor().getClass());
    }


    public void testBeanObjectStore() throws Exception
    {
        testPojoObjectStore("beanObjectStore");
    }

    private void testPojoObjectStore(final String flowName) throws Exception
    {
        final IdempotentMessageFilter filter = idempotentMessageFilterFromFlow(flowName);

        final ObjectStore<?> store = filter.getStore();
        assertEquals(CustomObjectStore.class, store.getClass());

        final CustomObjectStore customStore = (CustomObjectStore) store;
        assertEquals("the-value:" + flowName, customStore.getCustomProperty());
    }

    private IdempotentMessageFilter idempotentMessageFilterFromFlow(final String flowName) throws Exception
    {
        final FlowConstruct flow = getFlowConstruct(flowName);
        assertTrue(flow instanceof Flow);

        final Flow simpleFlow = (Flow) flow;
        final List<MessageProcessor> processors = simpleFlow.getMessageProcessors();
        assertEquals(1, processors.size());

        final MessageProcessor firstMP = processors.get(0);
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

        public void setCustomProperty(final String value)
        {
            customProperty = value;
        }
    }
}
