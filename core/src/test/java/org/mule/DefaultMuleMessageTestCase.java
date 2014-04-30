/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.types.MimeTypes;
import org.mule.transport.NullPayload;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

public class DefaultMuleMessageTestCase extends AbstractMuleContextTestCase
{
    //
    // corner cases/errors
    //
    @Test
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

    @Test
    public void testConstructorWithNullPayload()
    {
        MuleMessage message = new DefaultMuleMessage(null, muleContext);
        assertEquals(NullPayload.getInstance(), message.getPayload());
    }

    //
    // payload-only ctor tests
    //
    @Test
    public void testOneArgConstructor()
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        assertEquals(TEST_MESSAGE, message.getPayload());
    }

    @Test
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
    @Test
    public void testMessagePropertiesConstructor()
    {
        Map<String, Object> properties = createMessageProperties();

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, properties, muleContext);
        assertEquals(TEST_MESSAGE, message.getPayload());
        assertOutboundMessageProperty("MessageProperties", message);
    }

    @Test
    public void testMessagePropertiesAccessors()
    {
        Map<String, Object> properties = createMessageProperties();

        properties.put("number", "24");
        properties.put("decimal", "24.3");
        properties.put("boolean", "true");
        Apple apple = new Apple(true);
        properties.put("apple", apple);
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, properties, muleContext);
        assertTrue(message.getOutboundProperty("boolean", false));
        assertEquals(new Integer(24), message.getOutboundProperty("number", 0));
        assertEquals(new Byte((byte) 24), message.getOutboundProperty("number", (byte) 0));
        assertEquals(new Long(24), message.getOutboundProperty("number", 0l));
        assertEquals(new Float(24.3), message.getOutboundProperty("decimal", 0f));
        Double d = message.getOutboundProperty("decimal", 0d);
        assertEquals(new Double(24.3), d);

        assertEquals("true", message.getOutboundProperty("boolean", ""));

        assertEquals(apple, message.getOutboundProperty("apple"));
        try
        {
            message.getOutboundProperty("apple", new Orange());
            fail("Orange is not assignable to Apple");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }

        //Test null
        assertNull(message.getOutboundProperty("banana"));
        assertNull(message.getOutboundProperty("blah"));

        //Test default value
        assertEquals(new Float(24.3), message.getOutboundProperty("blah", 24.3f));

    }

    @Test
    public void testMessagePropertiesConstructorWithMuleMessageAsPayload()
    {
        Map<String, Object> properties = createMessageProperties();
        MuleMessage previousMessage = createMuleMessage();

        MuleMessage message = new DefaultMuleMessage(previousMessage, properties, muleContext);
        assertEquals("MULE_MESSAGE", message.getPayload());
        assertOutboundMessageProperty("MessageProperties", message);
        assertOutboundMessageProperty("MuleMessage", message);
    }

    //
    // ctor with previous message
    //
    @Test
    public void testPreviousMessageConstructorWithRegularPayloadAndMuleMessageAsPrevious()
    {
        MuleMessage previous = createMuleMessage();

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, previous, muleContext);
        assertEquals(TEST_MESSAGE, message.getPayload());
        assertOutboundMessageProperty("MuleMessage", message);
        assertEquals(previous.getUniqueId(), message.getUniqueId());
    }

    @Test
    public void testPreviousMessageConstructorWithMuleMessageAsPayloadAndMuleMessageAsPrevious()
    {
        MuleMessage payload = createMuleMessage();
        payload.setOutboundProperty("payload", "payload");

        MuleMessage previous = createMuleMessage();
        previous.setOutboundProperty("previous", "previous");

        MuleMessage message = new DefaultMuleMessage(payload, previous, muleContext);
        assertEquals("MULE_MESSAGE", message.getPayload());
        assertOutboundMessageProperty("MuleMessage", message);
        assertOutboundMessageProperty("payload", message);
        assertEquals(previous.getUniqueId(), message.getUniqueId());
    }

    @Test
    public void testClearProperties()
    {
        MuleMessage payload = createMuleMessage();
        payload.setOutboundProperty("foo", "fooValue");
        payload.setInvocationProperty("bar", "barValue");

        assertEquals(1, payload.getInvocationPropertyNames().size());
        assertEquals(2, payload.getOutboundPropertyNames().size());
        assertEquals(0, payload.getInboundPropertyNames().size());

        payload.clearProperties(PropertyScope.INVOCATION);
        assertEquals(0, payload.getInvocationPropertyNames().size());

        payload.clearProperties(PropertyScope.OUTBOUND);
        assertEquals(0, payload.getOutboundPropertyNames().size());

        //See http://www.mulesoft.org/jira/browse/MULE-4968 for additional test needed here
    }

    //
    // copy ctor
    //
    @Test
    public void testCopyConstructor() throws Exception
    {
        DefaultMuleMessage original = (DefaultMuleMessage) createMuleMessage();
        Map<String, Object> properties = createMessageProperties();
        original.addInboundProperties(properties);
        assertInboundAndOutboundMessageProperties(original);

        MuleMessage copy = new DefaultMuleMessage(original);
        assertInboundAndOutboundMessageProperties(copy);
        assertEquals(muleContext.getConfiguration().getDefaultEncoding(),
            copy.getOutboundProperty(MuleProperties.MULE_ENCODING_PROPERTY));
        
        // Mutate original
        original.setProperty("FOO", "OTHER", PropertyScope.OUTBOUND);
        assertNull(copy.getProperty("FOO", PropertyScope.OUTBOUND));
        original.setProperty("FOO", "OTHER", PropertyScope.INBOUND);
        assertNull(copy.getProperty("FOO", PropertyScope.INBOUND));

        // Mutate copy
        copy.setProperty("ABC", "OTHER", PropertyScope.OUTBOUND);
        assertNull(original.getProperty("ABC", PropertyScope.OUTBOUND));
        copy.setProperty("ABC", "OTHER", PropertyScope.INBOUND);
        assertNull(original.getProperty("ABC", PropertyScope.INBOUND));

    }

    private void assertInboundAndOutboundMessageProperties(MuleMessage original)
    {
        assertOutboundMessageProperty("MuleMessage", original);
        assertEquals("MessageProperties", original.getInboundProperty("MessageProperties"));
    }

    //
    // attachments
    //
    @Test
    public void testLegacyAddingAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        message.addOutboundAttachment("attachment", handler);

        assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getOutboundAttachment("attachment"));
    }

    @Test
    public void testAddingOutboundAttachment() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        message.addOutboundAttachment("attachment", handler);

        assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getOutboundAttachment("attachment"));
        assertEquals(0, message.getInboundAttachmentNames().size());

        message.removeOutboundAttachment("attachment");
        assertEquals(0, message.getOutboundAttachmentNames().size());

        //Try with content type set
        message.addOutboundAttachment("spi-props", IOUtils.getResourceAsUrl("test-spi.properties", getClass()), MimeTypes.TEXT);

        assertTrue(message.getOutboundAttachmentNames().contains("spi-props"));
        handler = message.getOutboundAttachment("spi-props");
        assertEquals(MimeTypes.TEXT, handler.getContentType());
        assertEquals(1, message.getOutboundAttachmentNames().size());

        //Try without content type set
        message.addOutboundAttachment("dummy", IOUtils.getResourceAsUrl("dummy.xml", getClass()), null);
        handler = message.getOutboundAttachment("dummy");
        assertEquals(MimeTypes.APPLICATION_XML, handler.getContentType());
        assertEquals(2, message.getOutboundAttachmentNames().size());


    }

    @Test
    public void testAddingInboundAttachment() throws Exception
    {
        Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();

        String attachmentData = "this is the attachment";
        DataHandler dh = new DataHandler(attachmentData, "text/plain");
        attachments.put("attachment", dh);
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, null, attachments, muleContext);

        assertTrue(message.getInboundAttachmentNames().contains("attachment"));
        assertEquals(dh, message.getInboundAttachment("attachment"));
        assertEquals(0, message.getOutboundAttachmentNames().size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        oos.flush();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        MuleMessage message2 = (MuleMessage) ois.readObject();
        assertTrue(message2.getInboundAttachmentNames().contains("attachment"));
        assertEquals(message2.getInboundAttachment("attachment").getContent(), attachmentData);
        assertEquals(0, message2.getOutboundAttachmentNames().size());

    }

    @Test
    public void testNewMuleMessageFromMuleMessageWithAttachment() throws Exception
    {
        MuleMessage previous = createMuleMessage();
        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        previous.addOutboundAttachment("attachment", handler);

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, previous, muleContext);
        assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getOutboundAttachment("attachment"));
    }

    @Test
    public void testFindPropertiesInAnyScope() throws Exception
    {
        MuleMessage message = createMuleMessage();
        //Not sure why this test adds this property
        message.removeProperty("MuleMessage", PropertyScope.OUTBOUND);

        // We need a session and current event for this test
        RequestContext.setEvent(new DefaultMuleEvent(message, getTestInboundEndpoint("foo"), getTestService()));

        message.setOutboundProperty("foo", "fooOutbound");
        message.setInvocationProperty("bar", "barInvocation");
        message.setInvocationProperty("foo", "fooInvocation");
        message.setProperty("foo", "fooInbound", PropertyScope.INBOUND);
        message.setSessionProperty("foo", "fooSession");


        assertEquals(2, message.getInvocationPropertyNames().size());
        assertEquals(1, message.getOutboundPropertyNames().size());
        assertEquals(1, message.getInboundPropertyNames().size());
        assertEquals(1, message.getSessionPropertyNames().size());

        String value = message.findPropertyInAnyScope("foo", null);
        assertEquals("fooOutbound", value);

        message.removeProperty("foo", PropertyScope.OUTBOUND);

        value = message.findPropertyInAnyScope("foo", null);
        assertEquals("fooInvocation", value);

        message.removeProperty("foo", PropertyScope.INVOCATION);

        value = message.findPropertyInAnyScope("foo", null);
        assertEquals("fooSession", value);
        message.removeProperty("foo", PropertyScope.SESSION);

        value = message.findPropertyInAnyScope("foo", null);
        assertEquals("fooInbound", value);

        value = message.findPropertyInAnyScope("bar", null);
        assertEquals("barInvocation", value);

    }

    //
    // helpers
    //
    private Map<String, Object> createMessageProperties()
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("MessageProperties", "MessageProperties");
        return map;
    }

    private MuleMessage createMuleMessage()
    {
        MuleMessage previousMessage = new DefaultMuleMessage("MULE_MESSAGE", muleContext);
        previousMessage.setOutboundProperty("MuleMessage", "MuleMessage");
        return previousMessage;
    }

    private void assertOutboundMessageProperty(String key, MuleMessage message)
    {
        // taking advantage of the fact here that key and value are the same
        assertEquals(key, message.getOutboundProperty(key));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testPropertyNamesImmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.getPropertyNames().add("other");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testInboundPropertyNamesAddImmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.getPropertyNames(PropertyScope.INBOUND).add("other");
    }

    public void testInboundPropertyNamesRemoveMmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.setProperty("foo", "bar", PropertyScope.INBOUND);
        message.getPropertyNames(PropertyScope.INBOUND).remove("foo");
        assertNull(message.getInboundProperty("foo"));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testOutboundPropertyNamesImmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.getPropertyNames(PropertyScope.OUTBOUND).add("other");
    }

    public void testOutboundPropertyNamesRemoveMmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.setOutboundProperty("foo", "bar");
        message.getPropertyNames(PropertyScope.OUTBOUND).remove("foo");
        assertNull(message.getOutboundProperty("foo"));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testInvocationPropertyNamesImmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.getPropertyNames(PropertyScope.INVOCATION).add("other");
    }
    
    public void tesInvocationPropertyNamesRemoveMmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.setInvocationProperty("foo", "bar");
        message.getPropertyNames(PropertyScope.INVOCATION).remove("foo");
        assertNull(message.getInvocationProperty("foo"));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testSessionPropertyNamesImmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.getPropertyNames(PropertyScope.SESSION).add("other");
    }
    
    public void testtSessionPropertyNamesRemoveMmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.setSessionProperty("foo", "bar");
        message.getPropertyNames(PropertyScope.SESSION).remove("foo");
        assertNull(message.getSessionProperty("foo"));
    }
}
