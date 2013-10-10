/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import static org.junit.Assert.assertEquals;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.NullMessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class CompositeMessageProcessorDefinitionParserTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
