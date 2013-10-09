/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.param;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
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
        MuleMessage message = muleContext.getClient().send("vm://header", null, null);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals("barValue", result.get("bar"));
    }

    @Test
    public void testProcessHeaderWithExistingOutHeaders() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://header2", null, null);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals("barValue", result.get("bar"));
        assertEquals("fooValue", result.get("foo"));
    }

    @Test
    public void testInvalidParamType() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://invalid", null, null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(IllegalArgumentException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }
}
