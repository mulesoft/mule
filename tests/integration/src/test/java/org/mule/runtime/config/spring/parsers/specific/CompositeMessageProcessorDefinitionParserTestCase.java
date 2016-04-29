/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.specific;

import static org.junit.Assert.assertEquals;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.processor.NullMessageProcessor;

import org.junit.Test;

public class CompositeMessageProcessorDefinitionParserTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/specific/composite-message-processor.xml";
    }

    @Test
    public void testInterceptingComposite() throws Exception
    {
        MessageProcessor composite = muleContext.getRegistry().lookupObject("composite1");
        assertEquals("0123", composite.process(getTestEvent("0")).getMessageAsString());
    }

    @Test
    public void testInterceptingNestedComposite() throws Exception
    {
        MessageProcessor composite = muleContext.getRegistry().lookupObject("composite2");
        assertEquals("01abc2", composite.process(getTestEvent("0")).getMessageAsString());
    }

    @Test
    public void testInterceptingCompositeOnEndpoint() throws Exception
    {
        EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder("endpoint");
        InboundEndpoint endpoint = endpointBuilder.buildInboundEndpoint();
        assertEquals(2, endpoint.getMessageProcessors().size());

        MessageProcessor endpointProcessor = endpoint.getMessageProcessorsFactory()
            .createInboundMessageProcessorChain(endpoint, null, new NullMessageProcessor());

        assertEquals("01231abc2", endpointProcessor.process(getTestEvent("0")).getMessageAsString());
    }

}
