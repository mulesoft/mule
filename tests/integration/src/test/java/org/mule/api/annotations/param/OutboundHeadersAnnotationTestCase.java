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
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;
import org.mule.util.ExceptionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class OutboundHeadersAnnotationTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/annotations/outbound-headers-annotation-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/annotations/outbound-headers-annotation-flow.xml"}});
    }

    public OutboundHeadersAnnotationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Test
    public void testProcessHeader() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://header", getTestMuleMessage(NullPayload.getInstance()));
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals("barValue", result.get("bar"));
    }

    @Test
    public void testProcessHeaderWithExistingOutHeaders() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://header2", getTestMuleMessage(NullPayload.getInstance()));
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals("barValue", result.get("bar"));
        assertEquals("fooValue", result.get("foo"));
    }

    @Test
    public void testInvalidParamType() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://invalid", getTestMuleMessage(NullPayload.getInstance()));
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(IllegalArgumentException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }
}
