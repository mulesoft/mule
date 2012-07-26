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

import junit.framework.Assert;

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
        assertTrue(evaluate("message.inboundAttachments", message) instanceof Map);
    }

    @Test
    public void assignToInboundAttachmentMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertFinalProperty("message.inboundAttachments='foo'", message);
    }

    @Test
    public void inboundAttachment() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        DataHandler dataHandler = Mockito.mock(DataHandler.class);
        message.addInboundAttachment("foo", dataHandler);
        assertEquals(dataHandler, evaluate("message.inboundAttachments['foo']", message));
    }

    @Test
    public void assignValueToInboundAttachment() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        DataHandler dataHandler = Mockito.mock(DataHandler.class);
        message.addInboundAttachment("foo", dataHandler);
        assertUnsupportedOperation("message.inboundAttachments['foo']=new DataHandler('bar','text/plain')",
            message);
    }

    @Test
    public void assignValueToNewInboundAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertUnsupportedOperation(
            "message.inboundAttachments['foo_new']=new DataHandler('bar','text/plain')", message);
    }

    @Test
    public void outboundAttachmentMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertTrue(evaluate("message.outboundAttachments", message) instanceof Map);
    }

    @Test
    public void assignToOutboundAttachmentMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertFinalProperty("message.outboundAttachments='foo'", message);
    }

    @Test
    public void outboundAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        DataHandler dataHandler = Mockito.mock(DataHandler.class);
        message.addOutboundAttachment("foo", dataHandler);
        assertEquals(dataHandler, evaluate("message.outboundAttachments['foo']", message));
    }

    @Test
    public void assignValueToOutboundAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.addOutboundAttachment("foo", Mockito.mock(DataHandler.class));
        evaluate("message.outboundAttachments['foo']=new DataHandler('bar','text/plain')", message);
        assertEquals("bar", message.getOutboundAttachment("foo").getContent());
    }

    @Test
    public void assignValueToNewOutboundAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        evaluate("message.outboundAttachments['foo']=new DataHandler('bar','text/plain')", message);
        assertEquals("bar", message.getOutboundAttachment("foo").getContent());
    }

    @Test
    public void inboundClear() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.clear()", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void inboundSize() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.addInboundAttachment("foo", Mockito.mock(DataHandler.class));
        message.addInboundAttachment("bar", Mockito.mock(DataHandler.class));
        assertEquals(2, evaluate("message.inboundAttachments.size()", message));
    }

    @Test
    public void inboundKeySet() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.addInboundAttachment("foo", Mockito.mock(DataHandler.class));
        message.addInboundAttachment("bar", Mockito.mock(DataHandler.class));
        assertEquals("foo", evaluate("message.inboundAttachments.keySet().toArray()[0]", message));
        assertEquals("bar", evaluate("message.inboundAttachments.keySet().toArray()[1]", message));
    }

    @Test
    public void inboundContainsKey() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.addInboundAttachment("foo", Mockito.mock(DataHandler.class));
        Assert.assertTrue((Boolean)evaluate("message.inboundAttachments.containsKey('foo')", message));
        Assert.assertFalse((Boolean)evaluate("message.inboundAttachments.containsKey('bar')", message));
    }

    @Test
    public void inboundContainsValue() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.containsValue('bar')",
            Mockito.mock(MuleMessage.class));
    }

    @Test
    public void inboundEntrySet() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.entrySet()", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void inboundValues() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.values()", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void inboundIsEmpty() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        Assert.assertTrue((Boolean)evaluate("message.inboundAttachments.isEmpty()", message));
        message.addInboundAttachment("foo", Mockito.mock(DataHandler.class));
        message.addInboundAttachment("bar", Mockito.mock(DataHandler.class));
        Assert.assertFalse((Boolean)evaluate("message.inboundAttachments.isEmpty()", message));
    }

    @Test
    public void inboundPutAll() throws Exception
    {
        assertUnsupportedOperation(
            "message.inboundAttachments.putAll(['foo': new DataHandler(new URL('http://val1')),'bar': new DataHandler(new URL('http://val2'))])",
            Mockito.mock(MuleMessage.class));
    }

    @Test
    public void inboundRemove() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.remove('foo')",
            Mockito.mock(MuleMessage.class));
    }

    @Test
    public void outboundClear() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.addOutboundAttachment("foo", Mockito.mock(DataHandler.class));
        message.addOutboundAttachment("bar", Mockito.mock(DataHandler.class));
        evaluate("message.outboundAttachments.clear()", message);
        assertEquals(0, message.getOutboundAttachmentNames().size());
    }

    @Test
    public void outboundSize() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.addOutboundAttachment("foo", Mockito.mock(DataHandler.class));
        message.addOutboundAttachment("bar", Mockito.mock(DataHandler.class));
        assertEquals(2, evaluate("message.outboundAttachments.size()", message));
    }

    @Test
    public void outboundKeySet() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.addOutboundAttachment("foo", Mockito.mock(DataHandler.class));
        message.addOutboundAttachment("bar", Mockito.mock(DataHandler.class));
        assertEquals("foo", evaluate("message.outboundAttachments.keySet().toArray()[0]", message));
        assertEquals("bar", evaluate("message.outboundAttachments.keySet().toArray()[1]", message));
    }

    @Test
    public void outboundContainsKey() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.addOutboundAttachment("foo", Mockito.mock(DataHandler.class));
        Assert.assertTrue((Boolean)evaluate("message.outboundAttachments.containsKey('foo')", message));
        Assert.assertFalse((Boolean)evaluate("message.outboundAttachments.containsKey('bar')", message));
    }

    @Test
    public void outboundContainsValue() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.containsValue('bar')",
            Mockito.mock(MuleMessage.class));
    }

    @Test
    public void outboundEntrySet() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.entrySet()", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void outboundValues() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.values()", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void outboundIsEmpty() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        Assert.assertTrue((Boolean)evaluate("message.outboundAttachments.isEmpty()", message));
        message.addOutboundAttachment("foo", Mockito.mock(DataHandler.class));
        message.addOutboundAttachment("bar", Mockito.mock(DataHandler.class));
        Assert.assertFalse((Boolean)evaluate("message.outboundAttachments.isEmpty()", message));
    }

    @Test
    public void outboundPutAll() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        evaluate(
            "message.outboundAttachments.putAll(['foo': new DataHandler(new URL('http://val1')),'bar': new DataHandler(new URL('http://val2'))])",
            message);
        assertEquals(DataHandler.class,
            ((DataHandler)evaluate("message.outboundAttachments['foo']", message)).getClass());
        assertEquals(DataHandler.class, evaluate("message.outboundAttachments['bar']", message).getClass());
    }

    @Test
    public void outboundInboundRemove() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.addOutboundAttachment("foo", Mockito.mock(DataHandler.class));
        Assert.assertFalse((Boolean)evaluate("message.outboundAttachments.isEmpty()", message));
        evaluate("message.outboundAttachments.remove('foo')", message);
        Assert.assertTrue((Boolean)evaluate("message.outboundAttachments.isEmpty()", message));
    }

}
