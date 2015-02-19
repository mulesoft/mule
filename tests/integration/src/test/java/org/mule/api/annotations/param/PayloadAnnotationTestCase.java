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
import org.mule.api.client.MuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;
import org.mule.util.ExceptionUtils;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class PayloadAnnotationTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/annotations/payload-annotation-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/annotations/payload-annotation-flow.xml"}});
    }

    public PayloadAnnotationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Test
    public void testPayloadNoTransform() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://payload1", "foo", null);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a String", message.getPayload() instanceof String);
        assertEquals("foo", message.getPayload());
    }

    @Test
    public void testPayloadAutoTransform() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://payload2", "foo", null);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a String", message.getPayload() instanceof InputStream);
        assertEquals("foo", IOUtils.toString((InputStream) message.getPayload()));
    }

    @Test
    public void testPayloadFailedTransform() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://payload3", getTestMuleMessage(NullPayload.getInstance()));
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(TransformerException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }
}
