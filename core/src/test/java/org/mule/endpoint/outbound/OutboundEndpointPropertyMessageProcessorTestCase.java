/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.AbstractMessageProcessorTestCase;

public class OutboundEndpointPropertyMessageProcessorTestCase extends AbstractMessageProcessorTestCase
{

    public void testProcess() throws InitialisationException, EndpointException, Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        MessageProcessor mp = new OutboundEndpointPropertyMessageProcessor(endpoint);

        MuleEvent event = mp.process(createTestOutboundEvent());

        assertEquals(endpoint.getEndpointURI().getUri().toString(),
                     event.getMessage().getOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY));
        assertSame(event, RequestContext.getEvent());
    }

}
