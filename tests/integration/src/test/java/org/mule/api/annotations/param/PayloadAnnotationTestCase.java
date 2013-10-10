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
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://payload1", "foo", null);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a String", message.getPayload() instanceof String);
        assertEquals("foo", message.getPayload());
    }

    @Test
    public void testPayloadAutoTransform() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://payload2", "foo", null);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a String", message.getPayload() instanceof InputStream);
        assertEquals("foo", IOUtils.toString((InputStream) message.getPayload()));
    }

    @Test
    public void testPayloadFailedTransform() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://payload3", null, null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(TransformerException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }
}
