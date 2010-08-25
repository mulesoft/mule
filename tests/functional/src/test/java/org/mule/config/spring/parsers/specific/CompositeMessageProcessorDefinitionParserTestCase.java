/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.NullMessageProcessor;
import org.mule.tck.FunctionalTestCase;

public class CompositeMessageProcessorDefinitionParserTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/specific/composite-message-processor.xml";
    }

    public void testInterceptingComposite() throws MuleException, Exception
    {
        MessageProcessor composite = muleContext.getRegistry().lookupObject("composite1");
        assertEquals("0123", composite.process(getTestEvent("0")).getMessageAsString());
    }

    public void testInterceptingNestedComposite() throws MuleException, Exception
    {
        MessageProcessor composite = muleContext.getRegistry().lookupObject("composite2");
        assertEquals("01abc2", composite.process(getTestEvent("0")).getMessageAsString());
    }

    public void testInterceptingCompositeOnEndpoint() throws MuleException, Exception
    {
        EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder("endpoint");
        InboundEndpoint endpoint = endpointBuilder.buildInboundEndpoint();
        assertEquals(2, endpoint.getMessageProcessors().size());

        MessageProcessor endpointProcessor = endpoint.getMessageProcessorsFactory()
            .createInboundMessageProcessorChain(endpoint, null, new NullMessageProcessor());

        assertEquals("01231abc2", endpointProcessor.process(getTestEvent("0")).getMessageAsString());
    }

}
