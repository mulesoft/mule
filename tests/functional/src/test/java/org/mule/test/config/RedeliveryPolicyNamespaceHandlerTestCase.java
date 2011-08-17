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
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.store.ObjectStore;
import org.mule.construct.Flow;
import org.mule.processor.RedeliveryPolicy;
import org.mule.processor.chain.AbstractMessageProcessorChain;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.SystemUtils;
import org.mule.util.store.InMemoryObjectStore;
import org.mule.util.store.SimpleMemoryObjectStore;
import org.mule.util.store.TextFileObjectStore;

import java.io.File;
import java.io.Serializable;
import java.util.List;

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

    public void testInMemoryObjectStore() throws Exception
    {
        RedeliveryPolicy filter = redeliveryPolicyFromFlow("inMemoryStore");

        assertNotNull(filter.getTheFailedMessageProcessor());
        assertEquals(12, filter.getMaxRedeliveryCount());
    }

    public void testSimpleTextFileStore() throws Exception
    {
        RedeliveryPolicy filter = redeliveryPolicyFromFlow("simpleTextFileStore");
        assertEquals("#[message:id]", filter.getIdExpression());
    }

    public void testCustomObjectStore() throws Exception
    {
        RedeliveryPolicy filter = redeliveryPolicyFromFlow("customObjectStore");
        assertNotNull(filter.getTheFailedMessageProcessor());
;
    }

    private RedeliveryPolicy redeliveryPolicyFromFlow(String flowName) throws Exception
    {
        FlowConstruct flow = getFlowConstruct(flowName);
        assertTrue(flow instanceof Flow);

        Flow simpleFlow = (Flow) flow;
        List<MessageProcessor> processors = simpleFlow.getMessageProcessors();
        assertEquals(1, processors.size());

        MessageProcessor firstMP = processors.get(0);
        assertEquals(RedeliveryPolicy.class, firstMP.getClass());

        return (RedeliveryPolicy) firstMP;
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
