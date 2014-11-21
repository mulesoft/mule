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
import org.mule.api.client.MuleClient;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;
import org.mule.util.ExceptionUtils;
import org.mule.util.IOUtils;

import java.io.InputStream;

import org.junit.Test;

public class PayloadAnnotationTestCase extends FunctionalTestCase
{

    public PayloadAnnotationTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/annotations/payload-annotation-flow.xml";
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
        MuleMessage message = client.send("vm://payload3", new DefaultMuleMessage(NullPayload.getInstance(), muleContext));
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(TransformerException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }
}
