/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;
import org.mule.util.ExceptionUtils;

import java.util.Map;

import org.junit.Test;

public class OutboundHeadersAnnotationTestCase extends FunctionalTestCase
{

    public OutboundHeadersAnnotationTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/annotations/outbound-headers-annotation-flow.xml";
    }

    @Test
    public void testProcessHeader() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://header", new DefaultMuleMessage(NullPayload.getInstance(), muleContext));
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals("barValue", result.get("bar"));
    }

    @Test
    public void testProcessHeaderWithExistingOutHeaders() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://header2", new DefaultMuleMessage(NullPayload.getInstance(), muleContext));
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals("barValue", result.get("bar"));
        assertEquals("fooValue", result.get("foo"));
    }

    @Test
    public void testInvalidParamType() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://invalid", new DefaultMuleMessage(NullPayload.getInstance(), muleContext));
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(IllegalArgumentException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }
}
