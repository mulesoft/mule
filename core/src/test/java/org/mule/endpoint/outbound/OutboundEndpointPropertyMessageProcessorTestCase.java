/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
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

public class OutboundEndpointPropertyMessageProcessorTestCase extends AbstractOutboundMessageProcessorTestCase
{

    public void testProcess() throws InitialisationException, EndpointException, Exception
    {
        MessageProcessor mp = new OutboundEndpointPropertyMessageProcessor();
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);

        MuleEvent event = mp.process(createTestOutboundEvent(endpoint, true));

        assertEquals(endpoint.getEndpointURI().getUri().toString(), event.getMessage().getStringProperty(
            MuleProperties.MULE_ENDPOINT_PROPERTY, null));
        assertSame(event, RequestContext.getEvent());
    }

}
