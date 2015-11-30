/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.inbound;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.InboundEndpoint;
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
        InboundExceptionDetailsMessageProcessor mp = new InboundExceptionDetailsMessageProcessor(endpoint.getConnector());
        mp.setMuleContext(muleContext);
        MuleEvent event = createTestInboundEvent(endpoint);
        event.getMessage().setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        MuleEvent result = mp.process(event);

        assertNotNull(result);
        final int status = result.getMessage().getOutboundProperty("status", 0);
        assertEquals(500, status);
    }

}
