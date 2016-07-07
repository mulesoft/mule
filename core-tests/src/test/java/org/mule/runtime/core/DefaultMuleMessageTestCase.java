/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

public class DefaultMuleMessageTestCase extends AbstractMuleContextTestCase
{

    public static final String FOO_PROPERTY = "foo";
    private Attributes testAttributes = new Attributes() {};

    @Test
    public void testMessagePropertiesAccessors()
    {
        Map<String, Serializable> properties = createMessageProperties();

        properties.put("number", "24");
        properties.put("decimal", "24.3");
        properties.put("boolean", "true");
        Apple apple = new Apple(true);
        properties.put("apple", apple);
        MuleMessage message = MuleMessage.builder().payload(TEST_MESSAGE).outboundProperties(properties).build();
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
    public void testClearProperties()
    {
        MuleMessage payload = MuleMessage.builder(createMuleMessage()).addOutboundProperty(FOO_PROPERTY, "fooValue").build();

        assertThat(payload.getOutboundPropertyNames(), hasSize(2));
        assertThat(payload.getInboundPropertyNames(), empty());

        payload = MuleMessage.builder(payload).outboundProperties(emptyMap()).build();
        assertThat(payload.getOutboundPropertyNames(), empty());

        //See http://www.mulesoft.org/jira/browse/MULE-4968 for additional test needed here
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
        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        MuleMessage message = MuleMessage.builder().payload(TEST_MESSAGE).addOutboundAttachment("attachment", handler).build();

        assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getOutboundAttachment("attachment"));
    }

    @Test
    public void testAddingOutboundAttachment() throws Exception
    {
        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        MuleMessage message = MuleMessage.builder().payload(TEST_MESSAGE).addOutboundAttachment("attachment", handler).build();

        assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
        assertEquals(handler, message.getOutboundAttachment("attachment"));
        assertEquals(0, message.getInboundAttachmentNames().size());

        message = MuleMessage.builder(message).removeOutboundAttachment("attachment").build();
        assertEquals(0, message.getOutboundAttachmentNames().size());

        message = MuleMessage.builder(message)
                             .addOutboundAttachment("spi-props", IOUtils.toDataHandler("spi-props", IOUtils.getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT))
                             .build();


        assertTrue(message.getOutboundAttachmentNames().contains("spi-props"));
        handler = message.getOutboundAttachment("spi-props");
        assertEquals(MediaType.TEXT.getPrimaryType(), handler.getContentType().split("/")[0]);
        assertEquals(MediaType.TEXT.getSubType(), handler.getContentType().split("/")[1]);
        assertEquals(1, message.getOutboundAttachmentNames().size());

        message = MuleMessage.builder(message)
                             .addOutboundAttachment("dummy", IOUtils.toDataHandler("dummy", IOUtils.getResourceAsUrl("dummy.xml", getClass()), null))
                             .build();
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
        MuleMessage message = MuleMessage.builder().payload(TEST_MESSAGE).inboundAttachments(attachments).build();

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
        MuleMessage previous = createMuleMessage();

        DataHandler handler = new DataHandler("this is the attachment", "text/plain");
        MuleMessage message = MuleMessage.builder(previous).payload(TEST_MESSAGE).addOutboundAttachment("attachment", handler).build();

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

    private MuleMessage createMuleMessage()
    {
        return MuleMessage.builder().payload(TEST_PAYLOAD).attributes(testAttributes).addOutboundProperty("MuleMessage", "MuleMessage").build();
    }

    private void assertOutboundMessageProperty(String key, MuleMessage message)
    {
        // taking advantage of the fact here that key and value are the same
        assertThat(message.getOutboundProperty(key), is(key));
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
        MuleMessage message = createMuleMessage();
        // message.setOutboundProperty(FOO_PROPERTY, "bar");
        // message.getOutboundPropertyNames().remove(FOO_PROPERTY);
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
        MuleMessage message = createMuleMessage();
        // message.setOutboundProperty(FOO_PROPERTY, "bar");
        // message.getOutboundPropertyNames().remove(FOO_PROPERTY);
        assertNull(message.getOutboundProperty(FOO_PROPERTY));
    }

    @Test
    public void usesNullPayloadAsNull() throws Exception
    {
        MuleMessage message = MuleMessage.builder(createMuleMessage()).addOutboundProperty(FOO_PROPERTY, NullPayload.getInstance()).build();

        assertThat(message.getOutboundProperty(FOO_PROPERTY), is(NullPayload.getInstance()));
    }

}
