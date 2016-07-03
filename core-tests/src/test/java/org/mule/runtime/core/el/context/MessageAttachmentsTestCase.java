/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.MediaType.ANY;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MutableMuleMessage;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.DataHandler;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.junit.Before;
import org.junit.Test;

public class MessageAttachmentsTestCase extends AbstractELTestCase
{

    private MuleEvent event;
    private MuleMessage message;

    public MessageAttachmentsTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Before
    public void setup()
    {
        event = mock(MuleEvent.class);
        message = mock(MuleMessage.class);
        when(event.getMessage()).thenReturn(message);
    }

    @Test
    public void inboundAttachmentMap() throws Exception
    {
        MuleEvent event = getTestEvent("");
        assertTrue(evaluate("message.inboundAttachments", event) instanceof Map);
    }

    @Test
    public void assignToInboundAttachmentMap() throws Exception
    {
        MuleEvent event = getTestEvent("");
        assertFinalProperty("message.inboundAttachments='foo'", event);
    }

    @Test
    public void inboundAttachment() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        DataHandler dataHandler = mock(DataHandler.class);

        event.setMessage(MuleMessage.builder(message).addInboundAttachment("foo", dataHandler).build());
        assertEquals(dataHandler, evaluate("message.inboundAttachments['foo']", event));
    }

    @Test
    public void assignValueToInboundAttachment() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        DataHandler dataHandler = mock(DataHandler.class);

        event.setMessage(MuleMessage.builder(message).addInboundAttachment("foo", dataHandler).build());
        assertUnsupportedOperation("message.inboundAttachments['foo']=new DataHandler('bar','text/plain')",
            event);
    }

    @Test
    public void assignValueToNewInboundAttachment() throws Exception
    {
        MuleEvent event = getTestEvent("");
        assertUnsupportedOperation(
            "message.inboundAttachments['foo_new']=new DataHandler('bar','text/plain')", event);
    }

    @Test
    public void outboundAttachmentMap() throws Exception
    {
        MuleEvent event = getTestEvent("");
        assertTrue(evaluate("message.outboundAttachments", event) instanceof Map);
    }

    @Test
    public void assignToOutboundAttachmentMap() throws Exception
    {
        MuleEvent event = getTestEvent("");
        assertFinalProperty("message.outboundAttachments='foo'", event);
    }

    @Test
    public void outboundAttachment() throws Exception
    {
        MuleEvent event = getTestEvent("");
        DataHandler dataHandler = mock(DataHandler.class);
        ((MutableMuleMessage) event.getMessage()).addOutboundAttachment("foo", dataHandler);
        assertEquals(dataHandler, evaluate("message.outboundAttachments['foo']", event));
    }

    @Test
    public void assignValueToOutboundAttachment() throws Exception
    {
        MuleEvent event = getTestEvent("");
        ((MutableMuleMessage) event.getMessage()).addOutboundAttachment("foo", mock(DataHandler.class));
        evaluate("message.outboundAttachments['foo']=new DataHandler('bar','text/plain')", event);
        assertEquals("bar", event.getMessage().getOutboundAttachment("foo").getContent());
    }

    @Test
    public void assignValueToNewOutboundAttachment() throws Exception
    {
        MuleEvent event = getTestEvent("");
        evaluate("message.outboundAttachments['foo']=new DataHandler('bar','text/plain')", event);
        assertEquals("bar", event.getMessage().getOutboundAttachment("foo").getContent());
    }

    @Test
    public void inboundClear() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.clear()", event);
    }

    @Test
    public void inboundSize() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();

        event.setMessage(MuleMessage.builder(message)
                                    .addInboundAttachment("foo", mock(DataHandler.class))
                                    .addInboundAttachment("bar", mock(DataHandler.class))
                                    .build());
        assertEquals(2, evaluate("message.inboundAttachments.size()", event));
    }

    @Test
    public void inboundKeySet() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();

        event.setMessage(MuleMessage.builder(message)
                                    .addInboundAttachment("foo", mock(DataHandler.class))
                                    .addInboundAttachment("bar", mock(DataHandler.class))
                                    .build());
        assertEquals(2, evaluate("message.inboundAttachments.keySet().size()", event));
        assertTrue((Boolean)evaluate("message.inboundAttachments.keySet().contains('foo')", event));
        assertTrue((Boolean) evaluate("message.inboundAttachments.keySet().contains('bar')", event));
    }

    @Test
    public void inboundContainsKey() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();

        event.setMessage(MuleMessage.builder(message)
                                    .addInboundAttachment("foo", mock(DataHandler.class))
                                    .build());

        assertTrue((Boolean) evaluate("message.inboundAttachments.containsKey('foo')", event));
        assertFalse((Boolean) evaluate("message.inboundAttachments.containsKey('bar')", event));
    }

    @Test
    public void inboundContainsValue() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        DataHandler valA = mock(DataHandler.class);
        when(valA.getContentType()).thenReturn(ANY.toString());

        event.setMessage(MuleMessage.builder(message)
                                    .payload(valA)
                                    .addInboundAttachment("foo", valA)
                                    .build());

        assertTrue((Boolean)evaluate("message.inboundAttachments.containsValue(payload)", event));
        assertFalse((Boolean)evaluate("message.inboundAttachments.containsValue('bar')", event));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void inboundEntrySet() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        DataHandler valA = mock(DataHandler.class);
        DataHandler valB = mock(DataHandler.class);

        event.setMessage(MuleMessage.builder(message)
                                    .addInboundAttachment("foo", valA)
                                    .addInboundAttachment("bar", valB)
                                    .build());
        Set<Map.Entry<String, DataHandler>> entrySet = (Set<Entry<String, DataHandler>>)evaluate(
            "message.inboundAttachments.entrySet()", event);
        assertEquals(2, entrySet.size());
        entrySet.contains(new DefaultMapEntry("foo", valA));
        entrySet.contains(new DefaultMapEntry("bar", valB));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void inboundValues() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        DataHandler valA = mock(DataHandler.class);
        DataHandler valB = mock(DataHandler.class);

        event.setMessage(MuleMessage.builder(message)
                                    .addInboundAttachment("foo", valA)
                                    .addInboundAttachment("bar", valB)
                                    .build());
        Collection<DataHandler> values = (Collection<DataHandler>) evaluate(
                "message.inboundAttachments.values()", event);
        assertEquals(2, values.size());
        values.contains(valA);
        values.contains(valB);
    }

    @Test
    public void inboundIsEmpty() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        assertTrue((Boolean) evaluate("message.inboundAttachments.isEmpty()", event));

        event.setMessage(MuleMessage.builder(message)
                                    .addInboundAttachment("foo", mock(DataHandler.class))
                                    .addInboundAttachment("bar", mock(DataHandler.class))
                                    .build());
        assertFalse((Boolean) evaluate("message.inboundAttachments.isEmpty()", event));
    }

    @Test
    public void inboundPutAll() throws Exception
    {
        assertUnsupportedOperation(
            "message.inboundAttachments.putAll(['foo': new DataHandler(new URL('http://val1')),'bar': new DataHandler(new URL('http://val2'))])",
            event);
    }

    @Test
    public void inboundRemove() throws Exception
    {
        assertUnsupportedOperation("message.inboundAttachments.remove('foo')", event);
    }

    @Test
    public void outboundClear() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();

        event.setMessage(MuleMessage.builder(message)
                                    .addOutboundAttachment("foo", mock(DataHandler.class))
                                    .addOutboundAttachment("bar", mock(DataHandler.class))
                                    .build());
        evaluate("message.outboundAttachments.clear()", event);
        assertEquals(0, event.getMessage().getOutboundAttachmentNames().size());
    }

    @Test
    public void outboundSize() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();

        event.setMessage(MuleMessage.builder(message)
                                    .addOutboundAttachment("foo", mock(DataHandler.class))
                                    .addOutboundAttachment("bar", mock(DataHandler.class))
                                    .build());
        assertEquals(2, evaluate("message.outboundAttachments.size()", event));
    }

    @Test
    public void outboundKeySet() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();

        event.setMessage(MuleMessage.builder(message)
                                    .addOutboundAttachment("foo", mock(DataHandler.class))
                                    .addOutboundAttachment("bar", mock(DataHandler.class))
                                    .build());
        assertEquals(2, evaluate("message.outboundAttachments.keySet().size()", event));
        assertTrue((Boolean) evaluate("message.outboundAttachments.keySet().contains('foo')", event));
        assertTrue((Boolean) evaluate("message.outboundAttachments.keySet().contains('bar')", event));
    }

    @Test
    public void outboundContainsKey() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();

        event.setMessage(MuleMessage.builder(message)
                                    .addOutboundAttachment("foo", mock(DataHandler.class))
                                    .build());
        assertTrue((Boolean) evaluate("message.outboundAttachments.containsKey('foo')", event));
        assertFalse((Boolean) evaluate("message.outboundAttachments.containsKey('bar')", event));
    }

    @Test
    public void outboundContainsValue() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        DataHandler valA = mock(DataHandler.class);
        when(valA.getContentType()).thenReturn(ANY.toString());

        event.setMessage(MuleMessage.builder(message)
                                    .payload(valA)
                                    .addOutboundAttachment("foo", valA)
                                    .build());
        assertTrue((Boolean) evaluate("message.outboundAttachments.containsValue(payload)", event));
        assertFalse((Boolean) evaluate("message.outboundAttachments.containsValue('bar')", event));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void outboundEntrySet() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        DataHandler valA = mock(DataHandler.class);
        DataHandler valB = mock(DataHandler.class);

        event.setMessage(MuleMessage.builder(message)
                                    .addOutboundAttachment("foo", valA)
                                    .addOutboundAttachment("bar", valB)
                                    .build());
        Set<Map.Entry<String, DataHandler>> entrySet = (Set<Entry<String, DataHandler>>)evaluate(
            "message.outboundAttachments.entrySet()", event);
        assertEquals(2, entrySet.size());
        entrySet.contains(new DefaultMapEntry("foo", valA));
        entrySet.contains(new DefaultMapEntry("bar", valB));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void outboundValues() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        DataHandler valA = mock(DataHandler.class);
        DataHandler valB = mock(DataHandler.class);

        event.setMessage(MuleMessage.builder(message)
                                    .addOutboundAttachment("foo", valA)
                                    .addOutboundAttachment("bar", valB)
                                    .build());
        Collection<DataHandler> values = (Collection<DataHandler>)evaluate(
            "message.outboundAttachments.values()", event);
        assertEquals(2, values.size());
        values.contains(valA);
        values.contains(valB);
    }

    @Test
    public void outboundIsEmpty() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        assertTrue((Boolean) evaluate("message.outboundAttachments.isEmpty()", event));

        event.setMessage(MuleMessage.builder(message)
                                    .addOutboundAttachment("foo", mock(DataHandler.class))
                                    .addOutboundAttachment("bar", mock(DataHandler.class))
                                    .build());
        assertFalse((Boolean) evaluate("message.outboundAttachments.isEmpty()", event));
    }

    @Test
    public void outboundPutAll() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        evaluate(
            "message.outboundAttachments.putAll(['foo': new DataHandler(new URL('http://val1')),'bar': new DataHandler(new URL('http://val2'))])",
            event);
        assertEquals(DataHandler.class,
            ((DataHandler)evaluate("message.outboundAttachments['foo']", event)).getClass());
        assertEquals(DataHandler.class, evaluate("message.outboundAttachments['bar']", event).getClass());
    }

    @Test
    public void outboundInboundRemove() throws Exception
    {
        MuleEvent event = getTestEvent("");
        final MuleMessage message = event.getMessage();

        event.setMessage(MuleMessage.builder(message)
                                    .addOutboundAttachment("foo", mock(DataHandler.class))
                                    .build());
        assertFalse((Boolean)evaluate("message.outboundAttachments.isEmpty()", event));
        evaluate("message.outboundAttachments.remove('foo')", event);
        assertTrue((Boolean)evaluate("message.outboundAttachments.isEmpty()", event));
    }

}
