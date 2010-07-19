/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

public class OutboundAttachmentsAnnotationTestCase extends FunctionalTestCase
{
    public OutboundAttachmentsAnnotationTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "outbound-attachments-annotation.xml";
    }


    @Test
    public void testProcessAttachment() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://attachment", null, null);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<String, DataHandler> result = (Map) message.getPayload();
        assertEquals("barValue", result.get("bar").getContent());
    }

    @Test
    public void testProcessAttachmentWithExistingOutAttachments() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://attachment2", null, null);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertTrue("Message payload should be a Map", message.getPayload() instanceof Map);
        Map<String, DataHandler> result = (Map) message.getPayload();
        assertEquals("barValue", result.get("bar").getContent());
        assertEquals("fooValue", result.get("foo").getContent());
    }

    @Test
    public void testInvalidParamType() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://invalid", null, null);
        assertNotNull("return message from MuleClient.send() should not be null", message);
        assertNotNull(message.getExceptionPayload());
        assertTrue(message.getExceptionPayload().getRootException() instanceof IllegalArgumentException);
    }
}
