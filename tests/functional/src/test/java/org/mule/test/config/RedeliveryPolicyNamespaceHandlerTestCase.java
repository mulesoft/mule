/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import org.junit.Test;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.construct.Flow;
import org.mule.processor.AbstractRedeliveryPolicy;
import org.mule.processor.IdempotentRedeliveryPolicy;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.SimpleMemoryObjectStore;

import java.io.Serializable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for all object stores that can be configured on an {@link org.mule.routing.IdempotentMessageFilter}.
 */
public class RedeliveryPolicyNamespaceHandlerTestCase extends FunctionalTestCase
{
    public RedeliveryPolicyNamespaceHandlerTestCase()
    {
        // we just test the wiring of the objects, no need to start the MuleContext
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/redelivery-policy-config.xml";
    }

    @Test
    public void testInMemoryObjectStore() throws Exception
    {
        IdempotentRedeliveryPolicy filter = redeliveryPolicyFromFlow("inMemoryStore");

        assertNotNull(filter.getTheFailedMessageProcessor());
        assertEquals(12, filter.getMaxRedeliveryCount());
        assertNull(filter.getIdExpression());
    }

    @Test
    public void testSimpleTextFileStore() throws Exception
    {
        IdempotentRedeliveryPolicy filter = redeliveryPolicyFromFlow("simpleTextFileStore");
        assertEquals("#[message:id]", filter.getIdExpression());
        assertNotNull(filter.getTheFailedMessageProcessor());
        assertEquals(5, filter.getMaxRedeliveryCount());
    }

    @Test
    public void testCustomObjectStore() throws Exception
    {
        IdempotentRedeliveryPolicy filter = redeliveryPolicyFromFlow("customObjectStore");
        assertNotNull(filter.getTheFailedMessageProcessor());
        assertEquals(5, filter.getMaxRedeliveryCount());
        assertNull(filter.getIdExpression());
    }

    private IdempotentRedeliveryPolicy redeliveryPolicyFromFlow(String flowName) throws Exception
    {
        FlowConstruct flow = getFlowConstruct(flowName);
        assertTrue(flow instanceof Flow);

        MessageSource source = ((Flow) flow).getMessageSource();
        assertTrue(source instanceof InboundEndpoint);
        AbstractRedeliveryPolicy redeliveryPolicy = ((InboundEndpoint)source).getRedeliveryPolicy();
        assertTrue(redeliveryPolicy instanceof IdempotentRedeliveryPolicy);
        return (IdempotentRedeliveryPolicy) redeliveryPolicy;
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
