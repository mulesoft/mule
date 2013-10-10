/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.inbound;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.AbstractMessageProcessorTestCase;
import org.mule.message.DefaultExceptionPayload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InboundExceptionDetailsProcessorTestCase extends AbstractMessageProcessorTestCase
{

    @Test
    public void testProcess() throws Exception
    {
        InboundEndpoint endpoint = createTestInboundEndpoint(null, null);
        MessageProcessor mp = new InboundExceptionDetailsMessageProcessor(endpoint.getConnector());

        MuleEvent event = createTestInboundEvent(endpoint);
        event.getMessage().setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        MuleEvent result = mp.process(event);

        assertNotNull(result);
        final int status = result.getMessage().getOutboundProperty("status", 0);
        assertEquals(500, status);
    }
}
