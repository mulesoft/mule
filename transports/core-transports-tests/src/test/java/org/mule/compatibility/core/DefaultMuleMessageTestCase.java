/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mule.runtime.core.PropertyScope.OUTBOUND;

import org.mule.compatibility.core.DefaultMuleEventEndpointUtils;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import org.junit.Test;

public class DefaultMuleMessageTestCase extends AbstractMuleContextEndpointTestCase
{

    public static final String FOO_PROPERTY = "foo";


    @Test
    public void testFindPropertiesInAnyScope() throws Exception
    {
        MuleMessage message = createMuleMessage();
        //Not sure why this test adds this property
        message.removeProperty("MuleMessage", OUTBOUND);

        // We need a session and current event for this test
        final DefaultMuleEvent event = new DefaultMuleEvent(message, getTestFlow());
        DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event, getTestInboundEndpoint(FOO_PROPERTY));
        RequestContext.setEvent(event);

        message.setOutboundProperty(FOO_PROPERTY, "fooOutbound");
        message.setProperty(FOO_PROPERTY, "fooInbound", PropertyScope.INBOUND);


        assertEquals(1, message.getOutboundPropertyNames().size());
        assertEquals(1, message.getInboundPropertyNames().size());

        String value = message.findPropertyInAnyScope(FOO_PROPERTY, null);
        assertEquals("fooOutbound", value);

        message.removeProperty(FOO_PROPERTY, OUTBOUND);

        value = message.findPropertyInAnyScope(FOO_PROPERTY, null);
        assertEquals("fooInbound", value);
    }

    private MuleMessage createMuleMessage()
    {
        MuleMessage previousMessage = new DefaultMuleMessage("MULE_MESSAGE", muleContext);
        previousMessage.setOutboundProperty("MuleMessage", "MuleMessage");
        return previousMessage;
    }

    public void testInboundPropertyNamesRemoveMmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.setProperty(FOO_PROPERTY, "bar", PropertyScope.INBOUND);
        message.getPropertyNames(PropertyScope.INBOUND).remove(FOO_PROPERTY);
        assertNull(message.getInboundProperty(FOO_PROPERTY));
    }

    public void testOutboundPropertyNamesRemoveMmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.setOutboundProperty(FOO_PROPERTY, "bar");
        message.getPropertyNames(OUTBOUND).remove(FOO_PROPERTY);
        assertNull(message.getOutboundProperty(FOO_PROPERTY));
    }
}
