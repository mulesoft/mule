/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MutableMuleMessage;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

public class DefaultMuleMessageTestCase extends AbstractMuleContextTestCase
{

    public static final String FOO_PROPERTY = "foo";
    private Serializable testAttributes = new Apple();

    @Test
    public void testConstructorWithNullPayload()
    {
        MuleMessage message = new DefaultMuleMessage(null, muleContext);
        assertThat(message.getPayload(), is(NullPayload.getInstance()));
    }

    //
    // payload-only ctor tests
    //
    @Test
    public void testOneArgConstructor()
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        assertThat(message.getPayload(), is(TEST_MESSAGE));
    }

    @Test
    public void testOneArgConstructorWithMuleMessageAsPayload()
    {
        MuleMessage oldMessage = createMuleMessage();

        MuleMessage message = new DefaultMuleMessage(oldMessage, muleContext);
        assertThat(message.getPayload(), is(AbstractMuleContextTestCase.TEST_PAYLOAD));
        assertOutboundMessageProperty("MuleMessage", message);
    }

    //
    // ctor with message properties
    //
    @Test
    public void testMessagePropertiesConstructor()
    {
        Map<String, Serializable> properties = createMessageProperties();

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, properties, muleContext);
        assertEquals(TEST_MESSAGE, message.getPayload());
        assertOutboundMessageProperty("MessageProperties", message);
    }

    @Test
    public void testMessagePropertiesAccessors()
    {
        Map<String, Serializable> properties = createMessageProperties();

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
        Map<String, Serializable> properties = createMessageProperties();
        MuleMessage previousMessage = createMuleMessage();

        MuleMessage message = new DefaultMuleMessage(previousMessage, properties, muleContext);
        assertEquals(AbstractMuleContextTestCase.TEST_PAYLOAD, message.getPayload());
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
        MutableMuleMessage payload = createMuleMessage();
        payload.setOutboundProperty("payload", "payload");

        MutableMuleMessage previous = createMuleMessage();
        previous.setOutboundProperty("previous", "previous");

        MuleMessage message = new DefaultMuleMessage(payload, previous, muleContext);
        assertEquals(AbstractMuleContextTestCase.TEST_PAYLOAD, message.getPayload());
        assertOutboundMessageProperty("MuleMessage", message);
        assertOutboundMessageProperty("payload", message);
        assertEquals(previous.getUniqueId(), message.getUniqueId());
    }

    @Test
    public void testClearProperties()
    {
        MutableMuleMessage payload = createMuleMessage();
        payload.setOutboundProperty(FOO_PROPERTY, "fooValue");

        assertEquals(2, payload.getOutboundPropertyNames().size());
        assertEquals(0, payload.getInboundPropertyNames().size());

        payload.clearOutboundProperties();
        assertEquals(0, payload.getOutboundPropertyNames().size());

        //See http://www.mulesoft.org/jira/browse/MULE-4968 for additional test needed here
    }

    //
    // copy ctor
    //
    @Test
    public void testCopyConstructor() throws Exception
    {
        MutableMuleMessage original = createMuleMessage();
        Map<String, Serializable> properties = createMessageProperties();
        original.addInboundProperties(properties);
        assertInboundAndOutboundMessageProperties(original);

        MutableMuleMessage copy = new DefaultMuleMessage(original);
        assertInboundAndOutboundMessageProperties(copy);
        assertThat(copy.getDataType().getMediaType().getCharset().get(), equalTo(Charset.forName(muleContext.getConfiguration().getDefaultEncoding())));
        assertThat(copy.getAttributes(), is(testAttributes));
        
        // Mutate original
        original.setOutboundProperty("FOO", "OTHER");
        assertThat(copy.getOutboundProperty("FOO"), is(nullValue()));
        original.setInboundProperty("FOO", "OTHER");
        assertThat(copy.getInboundProperty("FOO"), is(nullValue()));

        // Mutate copy
        copy.setOutboundProperty("ABC", "OTHER");
        assertThat(original.getOutboundProperty("ABC"), is(nullValue()));
        copy.setInboundProperty("ABC", "OTHER");
        assertThat(original.getInboundProperty("ABC"), is(nullValue()));

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
        // TODO MULE-9856 Replace with the builder
        MutableMuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        message.addOutboundAttachment("attachment", handler);

        assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getOutboundAttachment("attachment"));
    }

    @Test
    public void testAddingOutboundAttachment() throws Exception
    {
        // TODO MULE-9856 Replace with the builder
        MutableMuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        message.addOutboundAttachment("attachment", handler);

        assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getOutboundAttachment("attachment"));
        assertEquals(0, message.getInboundAttachmentNames().size());

        message.removeOutboundAttachment("attachment");
        assertEquals(0, message.getOutboundAttachmentNames().size());

        //Try with content type set
        message.addOutboundAttachment("spi-props", IOUtils.getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);

        assertTrue(message.getOutboundAttachmentNames().contains("spi-props"));
        handler = message.getOutboundAttachment("spi-props");
        assertEquals(MediaType.TEXT.getPrimaryType(), handler.getContentType().split("/")[0]);
        assertEquals(MediaType.TEXT.getSubType(), handler.getContentType().split("/")[1]);
        assertEquals(1, message.getOutboundAttachmentNames().size());

        //Try without content type set
        message.addOutboundAttachment("dummy", IOUtils.getResourceAsUrl("dummy.xml", getClass()), null);
        handler = message.getOutboundAttachment("dummy");
        assertEquals(MediaType.APPLICATION_XML.getPrimaryType(), handler.getContentType().split("/")[0]);
        assertEquals(MediaType.APPLICATION_XML.getSubType(), handler.getContentType().split("/")[1]);
        assertEquals(2, message.getOutboundAttachmentNames().size());


    }

    @Test
    public void testAddingInboundAttachment() throws Exception
    {
        Map<String, DataHandler> attachments = new HashMap<>();

        String attachmentData = "this is the attachment";
        DataHandler dh = new DataHandler(attachmentData, "text/plain");
        attachments.put("attachment", dh);
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, null, attachments, muleContext);

        assertTrue(message.getInboundAttachmentNames().contains("attachment"));
        assertEquals(dh, message.getInboundAttachment("attachment"));
        assertEquals(0, message.getOutboundAttachmentNames().size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        RequestContext.setEvent(new DefaultMuleEvent(message, getTestFlow()));
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
        MutableMuleMessage previous = createMuleMessage();
        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        previous.addOutboundAttachment("attachment", handler);

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, previous, muleContext);
        assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getOutboundAttachment("attachment"));
    }

    //
    // helpers
    //
    private Map<String, Serializable> createMessageProperties()
    {
        HashMap<String, Serializable> map = new HashMap<>();
        map.put("MessageProperties", "MessageProperties");
        return map;
    }

    private MutableMuleMessage createMuleMessage()
    {
        // TODO MULE-9856 Replace with the builder
        MutableMuleMessage previousMessage = new DefaultMuleMessage(AbstractMuleContextTestCase.TEST_PAYLOAD, DataType.builder(DataType.STRING).charset(getDefaultEncoding(muleContext)).build(),
                                                                    testAttributes, muleContext);
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
        message.getOutboundPropertyNames().add("other");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testInboundPropertyNamesAddImmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.getOutboundPropertyNames().add("other");
    }

    public void testInboundPropertyNamesRemoveMmutable() throws Exception
    {
        MutableMuleMessage message = createMuleMessage();
        message.setOutboundProperty(FOO_PROPERTY, "bar");
        message.getOutboundPropertyNames().remove(FOO_PROPERTY);
        assertNull(message.getInboundProperty(FOO_PROPERTY));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testOutboundPropertyNamesImmutable() throws Exception
    {
        MuleMessage message = createMuleMessage();
        message.getOutboundPropertyNames().add("other");
    }

    public void testOutboundPropertyNamesRemoveMmutable() throws Exception
    {
        MutableMuleMessage message = createMuleMessage();
        message.setOutboundProperty(FOO_PROPERTY, "bar");
        message.getOutboundPropertyNames().remove(FOO_PROPERTY);
        assertNull(message.getOutboundProperty(FOO_PROPERTY));
    }

    @Test
    public void usesNullPayloadAsNull() throws Exception
    {
        MutableMuleMessage message = createMuleMessage();
        message.setOutboundProperty(FOO_PROPERTY, NullPayload.getInstance());

        assertThat(message.getOutboundProperty(FOO_PROPERTY), is(nullValue()));
    }

    @Test
    public void copyProperty() throws Exception
    {
        MutableMuleMessage message = createMuleMessage();
        message.setInboundProperty(FOO_PROPERTY, "bar");
        message.copyProperty(FOO_PROPERTY);

        assertThat(message.getOutboundProperty(FOO_PROPERTY), equalTo("bar"));
    }

}
