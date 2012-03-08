/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;

import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;
import org.mockito.Mockito;

public class MessageAttachmentsTestCase extends AbstractELTestCase
{
    public MessageAttachmentsTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void inboundAttachmentMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertTrue(evaluate("inboundAttachments", message) instanceof Map);
    }

    @Test
    public void assignToInboundAttachmentMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertImmutableVariable("inboundAttachments='foo'", message);
    }

    @Test
    public void inboundAttachment() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        DataHandler dataHandler = Mockito.mock(DataHandler.class);
        message.addInboundAttachment("foo", dataHandler);
        assertEquals(dataHandler, evaluate("inboundAttachments['foo']", message));
    }

    @Test
    public void assignValueToInboundAttachment() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        DataHandler dataHandler = Mockito.mock(DataHandler.class);
        message.addInboundAttachment("foo", dataHandler);
        assertUnsupportedOperation("inboundAttachments['foo']=new DataHandler('bar','text/plain')", message);
    }

    @Test
    public void assignValueToNewInboundAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertUnsupportedOperation("inboundAttachments['foo_new']=new DataHandler('bar','text/plain')",
            message);
    }

    @Test
    public void outboundAttachmentMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertTrue(evaluate("outboundAttachments", message) instanceof Map);
    }

    @Test
    public void assignToOutboundAttachmentMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertImmutableVariable("outboundAttachments='foo'", message);
    }

    @Test
    public void outboundAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        DataHandler dataHandler = Mockito.mock(DataHandler.class);
        message.addOutboundAttachment("foo", dataHandler);
        assertEquals(dataHandler, evaluate("outboundAttachments['foo']", message));
    }

    @Test
    public void assignValueToOutboundAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.addOutboundAttachment("foo", Mockito.mock(DataHandler.class));
        evaluate("outboundAttachments['foo']=new DataHandler('bar','text/plain')", message);
        assertEquals("bar", message.getOutboundAttachment("foo").getContent());
    }

    @Test
    public void assignValueToNewOutboundAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        evaluate("outboundAttachments['foo']=new DataHandler('bar','text/plain')", message);
        assertEquals("bar", message.getOutboundAttachment("foo").getContent());
    }

}
