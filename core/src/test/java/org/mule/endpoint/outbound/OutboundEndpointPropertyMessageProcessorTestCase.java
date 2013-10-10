/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.AbstractMessageProcessorTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class OutboundEndpointPropertyMessageProcessorTestCase extends AbstractMessageProcessorTestCase
{

    @Test
    public void testProcess() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        MessageProcessor mp = new OutboundEndpointPropertyMessageProcessor(endpoint);

        MuleEvent event = mp.process(createTestOutboundEvent());

        assertEquals(endpoint.getEndpointURI().getUri().toString(),
                     event.getMessage().getOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY));
        assertSame(event, RequestContext.getEvent());
    }

}
