/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.construct.Flow;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestInboundTransformer;
import org.mule.tck.testmodels.mule.TestResponseTransformer;
import org.mule.transformer.simple.MessagePropertiesTransformer;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.List;

import org.junit.Test;

public class EndpointTranformersInAttributesTestCase extends FunctionalTestCase
{    

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/endpoint/endpoint-attribute-transformers-flow.xml";
    }

    @Test
    public void testGlobalEndpoint1() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep1");

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List<MessageProcessor> transformers = endpoint.getMessageProcessors();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseMessageProcessors();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);
    }

    @Test
    public void testGlobalEndpoint2() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint("ep2");

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        assertTrue(processors.get(1) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List<MessageProcessor> transformers = endpoint.getMessageProcessors();
        assertNotNull(transformers);
        assertEquals(2, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);
        assertTrue(transformers.get(1) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        assertTrue(processors.get(1) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseMessageProcessors();
        assertNotNull(transformers);
        assertEquals(2, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);
        assertTrue(transformers.get(1) instanceof TestResponseTransformer);
    }

    @Test
    public void testGlobalEndpoints() throws MuleException
    {
        Object flow = muleContext.getRegistry().lookupObject("globalEndpoints");
        ImmutableEndpoint endpoint = (ImmutableEndpoint) ((Flow) flow).getMessageSource();

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List<MessageProcessor> transformers = endpoint.getMessageProcessors();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseMessageProcessors();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);

        endpoint = (ImmutableEndpoint) ((Flow) flow).getMessageProcessors().get(0);

        processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        assertTrue(processors.get(1) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getMessageProcessors();
        assertNotNull(transformers);
        assertEquals(2, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);
        assertTrue(transformers.get(1) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        assertTrue(processors.get(1) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseMessageProcessors();
        assertNotNull(transformers);
        assertEquals(2, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);
        assertTrue(transformers.get(1) instanceof TestResponseTransformer);
    }

    @Test
    public void testLocalEndpoints() throws MuleException
    {
        Object flow = muleContext.getRegistry().lookupObject("localEndpoints");
        ImmutableEndpoint endpoint = (ImmutableEndpoint) ((Flow) flow).getMessageSource();

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        List<MessageProcessor> transformers = endpoint.getMessageProcessors();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseMessageProcessors();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);

        endpoint = (ImmutableEndpoint) ((Flow) flow).getMessageProcessors().get(0);

        processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestInboundTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getMessageProcessors();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestResponseTransformer);
        // For backwards-compatibility only
        transformers = endpoint.getResponseMessageProcessors();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertTrue(transformers.get(0) instanceof TestResponseTransformer);
    }

    @Test
    public void testTransformerRefsWithChildProcessors() throws MuleException
    {
        Object flow = muleContext.getRegistry().lookupObject("transformerRefsWithChildProcessors");
        ImmutableEndpoint endpoint = (ImmutableEndpoint) ((Flow) flow).getMessageSource();

        List<MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof StringAppendTransformer);
        assertTrue(processors.get(1) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof MessageProcessorChain);
        assertTrue(((MessageProcessorChain)processors.get(0)).getMessageProcessors().get(0) instanceof StringAppendTransformer);
        assertTrue(processors.get(1) instanceof TestResponseTransformer);

        endpoint = (ImmutableEndpoint) ((Flow) flow).getMessageProcessors().get(0);

        processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof MessagePropertiesTransformer);
        assertTrue(processors.get(1) instanceof TestInboundTransformer);

        processors = endpoint.getResponseMessageProcessors();
        assertNotNull(processors);
        assertEquals(2, processors.size());
        assertTrue(processors.get(0) instanceof MessageProcessorChain);
        assertTrue(((MessageProcessorChain)processors.get(0)).getMessageProcessors().get(0) instanceof MessagePropertiesTransformer);
        assertTrue(processors.get(1) instanceof TestResponseTransformer);
    }

}
