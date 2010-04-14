/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

public class DefaultMuleMessageTestCase extends AbstractMuleTestCase
{
    //
    // corner cases/errors
    //
    public void testConstructorWithNoMuleContext()
    {
        try
        {
            new DefaultMuleMessage(TEST_MESSAGE, null);
            fail("DefaultMuleMessage must fail when created with null MuleContext");
        }
        catch (IllegalArgumentException iae)
        {
            // this one was expected
        }
    }

    public void testConstructorWithNullPayload()
    {
        MuleMessage message = new DefaultMuleMessage(null, muleContext);
        assertEquals(NullPayload.getInstance(), message.getPayload());
    }
    
    //
    // payload-only ctor tests
    //
    public void testOneArgConstructor()
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        assertEquals(TEST_MESSAGE, message.getPayload());
    }
        
    public void testOneArgConstructorWithMessageAdapterAsPayload()
    {
        MessageAdapter adapter = createMessageAdapter();
        
        MuleMessage message = new DefaultMuleMessage(adapter, muleContext);        
        assertEquals("MESSAGE_ADAPTER", message.getPayload());
        assertOutboundMessageProperty("MessageAdapter", message);
    }

    public void testOneArgConstructorWithMuleMessageAsPayload()
    {
        MuleMessage oldMessage = createMuleMessage();
        
        MuleMessage message = new DefaultMuleMessage(oldMessage, muleContext);
        assertEquals("MULE_MESSAGE", message.getPayload());
        assertOutboundMessageProperty("MuleMessage", message);
    }
    
    //
    // ctor with message properties
    //
    public void testMessagePropertiesConstructor()
    {
        Map<String, Object> properties = createMessageProperties();
        
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, properties, muleContext);
        assertEquals(TEST_MESSAGE, message.getPayload());
        assertOutboundMessageProperty("MessageProperties", message);
    }

    public void testMessagePropertiesConstructorWithMessageAdapterAsPayload()
    {
        Map<String, Object> properties = createMessageProperties();
        MessageAdapter messageAdapter = createMessageAdapter();
        
        MuleMessage message = new DefaultMuleMessage(messageAdapter, properties, muleContext);
        assertEquals("MESSAGE_ADAPTER", message.getPayload());
        assertOutboundMessageProperty("MessageProperties", message);
        assertOutboundMessageProperty("MessageAdapter", message);
    }
    
    public void testMessagePropertiesConstructorWithMuleMessageAsPayload()
    {
        Map<String, Object> properties = createMessageProperties();
        MuleMessage previousMessage = createMuleMessage();
        
        MuleMessage message = new DefaultMuleMessage(previousMessage, properties, muleContext);
        assertEquals("MULE_MESSAGE", message.getPayload());
        assertOutboundMessageProperty("MessageProperties", message);
        assertOutboundMessageProperty("MuleMessage", message);
        assertNull(message.getProperty("MessageAdapter"));
    }
    
    //
    // ctor with previous message
    //
    public void testPreviousMessageConstructorWithRegularPayloadAndMessageAdapterAsPrevious()
    {
        MessageAdapter previous = createMessageAdapter();
        
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, previous, muleContext);
        assertEquals(TEST_MESSAGE, message.getPayload());
        assertOutboundMessageProperty("MessageAdapter", message);
    }
    
    public void testPreviousMessageConstructorWithRegularPayloadAndMuleMessageAsPrevious()
    {
        MuleMessage previous = createMuleMessage();
        
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, previous, muleContext);
        assertEquals(TEST_MESSAGE, message.getPayload());
        assertOutboundMessageProperty("MuleMessage", message);
    }
    
    public void testPreviousMessageConstructorWithMessageAdapterAsPayloadAndMessageAdapterAsPrevious()
    {
        MessageAdapter payload = createMessageAdapter();
        
        MessageAdapter previous = createMessageAdapter();
        previous.setProperty("previous", "previous");
        
        MuleMessage message = new DefaultMuleMessage(payload, previous, muleContext);
        assertEquals("MESSAGE_ADAPTER", message.getPayload());
        assertOutboundMessageProperty("MessageAdapter", message);
    }

    public void testPreviousMessageConstructorWithMessageAdapterAsPayloadAndMuleMessageAsPrevious()
    {
        MessageAdapter payload = createMessageAdapter();
        MuleMessage previous = createMuleMessage();
        
        MuleMessage message = new DefaultMuleMessage(payload, previous, muleContext);
        assertEquals("MESSAGE_ADAPTER", message.getPayload());
        assertOutboundMessageProperty("MessageAdapter", message);
        assertNull(message.getProperty("MuleMessage"));
    }

    public void testPreviousMessageConstructorWithMuleMessageAsPayloadAndMessageAdapterAsPrevious()
    {
        MuleMessage payload = createMuleMessage();
        MessageAdapter previous = createMessageAdapter();
        
        MuleMessage message = new DefaultMuleMessage(payload, previous, muleContext);
        assertEquals("MULE_MESSAGE", message.getPayload());
        assertOutboundMessageProperty("MuleMessage", message);
        assertNull(message.getProperty("MessageAdapter"));
    }
    
    public void testPreviousMessageConstructorWithMuleMessageAsPayloadAndMuleMessageAsPrevious()
    {
        MuleMessage payload = createMuleMessage();
        payload.setProperty("payload", "payload");
        
        MuleMessage previous = createMuleMessage();
        previous.setProperty("previous", "previous");
        
        MuleMessage message = new DefaultMuleMessage(payload, previous, muleContext);
        assertEquals("MULE_MESSAGE", message.getPayload());
        assertOutboundMessageProperty("MuleMessage", message);
        assertOutboundMessageProperty("payload", message);
        assertNull(message.getProperty("MessageAdapter"));
    }

    //
    // attachments
    //
    public void testAddingAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        
        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        message.addAttachment("attachment", handler);
        
        assertTrue(message.getAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getAttachment("attachment"));
    }
    
    public void testNewMuleMessageFromMessageAdapterWithAttachment() throws Exception
    {
        MessageAdapter previous = createMessageAdapter();
        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        previous.addAttachment("attachment", handler);
        
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, previous, muleContext);
        assertTrue(message.getAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getAttachment("attachment"));
    }
    
    //
    // helpers
    //
    private Map<String, Object> createMessageProperties()
    {
        HashMap<String,Object> map = new HashMap<String, Object>();
        map.put("MessageProperties", "MessageProperties");
        return map;
    }

    private MessageAdapter createMessageAdapter()
    {
        MessageAdapter adapter = new DefaultMessageAdapter("MESSAGE_ADAPTER");
        adapter.setProperty("MessageAdapter", "MessageAdapter");
        return adapter;
    }
    
    private MuleMessage createMuleMessage()
    {
        MuleMessage previousMessage = new DefaultMuleMessage("MULE_MESSAGE", muleContext);
        previousMessage.setProperty("MuleMessage", "MuleMessage");
        return previousMessage;
    }

    private void assertOutboundMessageProperty(String key, MuleMessage message)
    {
        // taking advantage of the fact here that key and value are the same
        assertEquals(key, message.getProperty(key, PropertyScope.OUTBOUND));
    }
}
