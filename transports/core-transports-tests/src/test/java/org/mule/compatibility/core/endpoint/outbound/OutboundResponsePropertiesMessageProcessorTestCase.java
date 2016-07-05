/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.AbstractEndpointBuilder;
import org.mule.compatibility.core.processor.AbstractMessageProcessorTestCase;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;

import org.junit.Test;

public class OutboundResponsePropertiesMessageProcessorTestCase extends AbstractMessageProcessorTestCase
{

    private static String MY_PROPERTY_KEY = "myProperty";
    private static String MY_PROPERTY_VAL = "myPropertyValue";
    private static String MULE_CORRELATION_ID_VAL = "152";

    @Test
    public void testProcess() throws Exception
    {
        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null);
        InterceptingMessageProcessor mp = new OutboundResponsePropertiesMessageProcessor(endpoint);
        mp.setListener(event ->
        {
            // return event with same payload but no properties
            try
            {
                return new DefaultMuleEvent(MuleMessage.builder().payload(event.getMessage().getPayload()).build(), REQUEST_RESPONSE, getTestFlow(),
                    getTestSession(null, muleContext));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });

        MuleEvent event = createTestOutboundEvent();
        event.setMessage(event.getMessage().transform(msg ->
        {
            msg.setOutboundProperty(MY_PROPERTY_KEY, MY_PROPERTY_VAL);
            msg.setOutboundProperty(MULE_CORRELATION_ID_PROPERTY, MULE_CORRELATION_ID_VAL);
            return msg;
        }));

        MuleEvent result = mp.process(event);

        assertNotNull(result);
        assertEquals(TEST_MESSAGE, result.getMessageAsString());
        assertEquals(MY_PROPERTY_VAL, result.getMessage().getOutboundProperty(MY_PROPERTY_KEY));
        assertEquals(MULE_CORRELATION_ID_VAL,
                     result.getMessage().getOutboundProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY));
    }

    @Override
    protected void customizeEndpointBuilder(EndpointBuilder endpointBuilder)
    {
        endpointBuilder.setProperty(AbstractEndpointBuilder.PROPERTY_RESPONSE_PROPERTIES, "myProperty");
    }
}
