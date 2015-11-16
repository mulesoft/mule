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
import static org.junit.Assert.fail;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.ExceptionUtils;
import org.mule.util.StringDataSource;

import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

public class MixedAnnotationsTestCase extends FunctionalTestCase
{
    private MuleMessage muleMessage;

    public MixedAnnotationsTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/annotations/mixed-annotations-flow.xml";
    }

    @Override
    public void doSetUp() throws Exception
    {
        super.doSetUp();

        Map<String, Object> props = new HashMap<String, Object>(3);
        props.put("foo", "fooValue");
        props.put("bar", "barValue");
        props.put("baz", "bazValue");

        muleMessage = new DefaultMuleMessage("test", props, muleContext);

        try
        {
            muleMessage.addOutboundAttachment("foo", new DataHandler(new StringDataSource("fooValue")));
            muleMessage.addOutboundAttachment("bar", new DataHandler(new StringDataSource("barValue")));
            muleMessage.addOutboundAttachment("baz", new DataHandler(new StringDataSource("bazValue")));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testProcessAllAnnotated() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://allAnnotated", muleMessage);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<?, ?> result = (Map<?, ?>) message.getPayload();
        assertEquals(3, result.size());

        // Payload
        assertEquals("test", result.get("payload"));

        // Headers
        assertNotNull(result.get("inboundHeaders"));
        Map<?, ?> headers = (Map<?, ?>) result.get("inboundHeaders");
        assertEquals(2, headers.size());
        assertEquals("fooValue", headers.get("foo"));
        assertEquals("barValue", headers.get("bar"));

        // Attachments
        assertNotNull(result.get("inboundAttachments"));
        Map<?, ?> attachments = (Map<?, ?>) result.get("inboundAttachments");
        assertEquals(3, attachments.size());
        assertNotNull(attachments.get("foo"));
        assertNotNull(attachments.get("bar"));
        assertNotNull(attachments.get("baz"));
    }

    @Test
    public void testPayloadNotAnnotated() throws Exception
    {
        // When using param annotations every param needs t obe annotated
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://someAnnotated", muleMessage);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(IllegalArgumentException.class,
            ExceptionUtils.getRootCause(message.getExceptionPayload().getException()).getClass());
    }
}
