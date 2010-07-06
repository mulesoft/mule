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

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.AbstractEndpointBuilder;

public class OutboundResponsePropertiesMessageProcessorTestCase extends
    AbstractOutboundMessageProcessorTestCase
{

    private static String MY_PROPERTY_KEY = "myProperty";
    private static String MY_PROPERTY_VAL = "myPropertyValue";
    private static String MULE_CORRELATION_ID_VAL = "152";

    public void testProcess() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        InterceptingMessageProcessor mp = new OutboundResponsePropertiesMessageProcessor(endpoint);
        mp.setListener(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                // return event with same payload but no properties
                return new DefaultMuleEvent(new DefaultMuleMessage(event.getMessage().getPayload(),
                    muleContext), null, null);
            }
        });

        MuleEvent event = createTestOutboundEvent(endpoint);
        event.getMessage().setStringProperty(MY_PROPERTY_KEY, MY_PROPERTY_VAL);
        event.getMessage().setStringProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY,
            MULE_CORRELATION_ID_VAL);
        MuleEvent result = mp.process(event);

        assertNotNull(result);
        assertEquals(TEST_MESSAGE, result.getMessageAsString());
        assertEquals(MY_PROPERTY_VAL, result.getMessage().getStringProperty(MY_PROPERTY_KEY, null));
        assertEquals(MULE_CORRELATION_ID_VAL, result.getMessage().getStringProperty(
            MuleProperties.MULE_CORRELATION_ID_PROPERTY, null));
    }

    @Override
    protected void customizeEndpointBuilder(EndpointBuilder endpointBuilder)
    {
        endpointBuilder.setProperty(AbstractEndpointBuilder.PROPERTY_RESPONSE_PROPERTIES, "myProperty");
    }
}
