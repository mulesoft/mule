/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.message.MultiPartPayload.BODY_PART_NAME;
import static org.mule.runtime.core.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.core.util.IOUtils.toMuleMessagePart;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class MultiPartPayloadTestCase extends AbstractMuleContextTestCase
{

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void stringAttachment() throws Exception
    {
        final MuleMessage attachmentPart = MuleMessage.builder()
                                                      .payload("this is the attachment")
                                                      .mediaType(MediaType.TEXT)
                                                      .attributes(new AttachmentAttributes("attachment"))
                                                      .build();

        MuleMessage message = MuleMessage.builder().payload(new MultiPartPayload(
                MuleMessage.builder().payload(TEST_PAYLOAD).build(),
                attachmentPart
        )).build();

        assertThat(((MultiPartPayload) message.getPayload()).getPartsNames(), hasItem("attachment"));
        assertThat(((MultiPartPayload) message.getPayload()).getPart("attachment"), sameInstance(attachmentPart));
    }

    @Test
    public void fromUrlAttachment() throws Exception
    {
        final MuleMessage attachmentPart = toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);

        MuleMessage message = MuleMessage.builder().payload(new MultiPartPayload(
                MuleMessage.builder().payload(TEST_PAYLOAD).build(),
                attachmentPart)).build();

        assertThat(((MultiPartPayload) message.getPayload()).getParts(), hasSize(2));
        assertThat(((MultiPartPayload) message.getPayload()).getPartsNames(), hasItem("spi-props"));
        assertThat(((MultiPartPayload) message.getPayload()).getPart("spi-props"), sameInstance(attachmentPart));

        assertThat(((MultiPartPayload) message.getPayload()).getPart("spi-props").getDataType().getMediaType(), is(MediaType.TEXT));
    }

    @Test
    public void xmlFromUrlAttachment() throws Exception
    {
        final MuleMessage attachmentPart1 = toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);
        final MuleMessage attachmentPart2 = toMuleMessagePart("dummy", getResourceAsUrl("dummy.xml", getClass()), null);

        MuleMessage message = MuleMessage.builder().payload(new MultiPartPayload(
                MuleMessage.builder().payload(TEST_PAYLOAD).build(),
                attachmentPart1, attachmentPart2)).build();

        assertThat(((MultiPartPayload) message.getPayload()).getParts(), hasSize(3));
        assertThat(((MultiPartPayload) message.getPayload()).getPart("dummy").getDataType().getMediaType(), is(MediaType.APPLICATION_XML));
    }

    @Test
    public void withBody() throws Exception
    {
        final MuleMessage attachmentPart = MuleMessage.builder()
                                                      .payload("this is the attachment")
                                                      .mediaType(MediaType.TEXT)
                                                      .attributes(new AttachmentAttributes("attachment"))
                                                      .build();

        final MuleMessage bodyPart = MuleMessage.builder().payload(TEST_PAYLOAD).build();

        MuleMessage message = MuleMessage.builder().payload(new MultiPartPayload(
                bodyPart,
                attachmentPart)).build();

        assertThat(((MultiPartPayload) message.getPayload()).getPartsNames(), hasItem(BODY_PART_NAME));
        assertThat(((MultiPartPayload) message.getPayload()).getPart(BODY_PART_NAME), sameInstance(bodyPart));
    }

    @Test
    public void withoutBody() throws Exception
    {
        final MuleMessage attachmentPart1 = MuleMessage.builder()
                                                       .payload("this is the attachment1")
                                                       .mediaType(MediaType.TEXT)
                                                       .attributes(new AttachmentAttributes("attachment1"))
                                                       .build();
        final MuleMessage attachmentPart2 = MuleMessage.builder()
                                                       .payload("this is the attachment2")
                                                       .mediaType(MediaType.TEXT)
                                                       .attributes(new AttachmentAttributes("attachment2"))
                                                       .build();

        MuleMessage message = MuleMessage.builder().payload(new MultiPartPayload(
                attachmentPart1,
                attachmentPart2)).build();

        assertThat(((MultiPartPayload) message.getPayload()).getPartsNames(), not(hasItem(BODY_PART_NAME)));
    }

    @Test
    public void multiPartPayloadSerialization() throws Exception
    {
        final MuleMessage attachmentPart = MuleMessage.builder()
                                                      .payload("this is the attachment")
                                                      .mediaType(MediaType.TEXT)
                                                      .attributes(new AttachmentAttributes("attachment"))
                                                      .build();

        MuleMessage message = MuleMessage.builder().payload(new MultiPartPayload(
                MuleMessage.builder().payload(TEST_PAYLOAD).build(),
                attachmentPart)).build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        RequestContext.setEvent(new DefaultMuleEvent(message, getTestFlow()));
        oos.writeObject(message);
        oos.flush();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        message = (MuleMessage) ois.readObject();

        assertThat(((MultiPartPayload) message.getPayload()).getPartsNames(), hasItem("attachment"));
        assertThat(((MultiPartPayload) message.getPayload()).getPart("attachment").getPayload(), is("this is the attachment"));
        assertThat(((MultiPartPayload) message.getPayload()).getPart("attachment"), equalTo(attachmentPart));
    }

    @Test
    public void multiPartPayloadStreamsSerialization() throws Exception
    {
        final MuleMessage attachmentPart = toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);
        assertThat(attachmentPart.getPayload(), instanceOf(InputStream.class));

        MuleMessage message = MuleMessage.builder().payload(new MultiPartPayload(
                MuleMessage.builder().payload(TEST_PAYLOAD).build(),
                attachmentPart)).build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        RequestContext.setEvent(new DefaultMuleEvent(message, getTestFlow()));
        oos.writeObject(message);
        oos.flush();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        message = (MuleMessage) ois.readObject();

        assertThat(((MultiPartPayload) message.getPayload()).getPartsNames(), hasItem("spi-props"));
        assertThat(((MultiPartPayload) message.getPayload()).getPart("spi-props").getPayload(), instanceOf(byte[].class));
        assertThat(((MultiPartPayload) message.getPayload()).getPart("spi-props"), equalTo(attachmentPart));
    }

    @Test
    public void getPayloadAsStringFails() throws Exception
    {
        final MuleMessage attachmentPart = MuleMessage.builder()
                                                      .payload("this is the attachment")
                                                      .mediaType(MediaType.TEXT)
                                                      .attributes(new AttachmentAttributes("attachment"))
                                                      .build();

        MuleMessage message = MuleMessage.builder().payload(new MultiPartPayload(
                MuleMessage.builder().payload(TEST_PAYLOAD).build(),
                attachmentPart)).build();

        expected.expect(TransformerException.class);
        expected.expectMessage("\"MultiPartPayload\" cannot be transformed to java.lang.String.");

        getPayloadAsString(message);
    }

    @Test
    public void getPayloadAsBytesFails() throws Exception
    {
        final MuleMessage attachmentPart = MuleMessage.builder()
                                                      .payload("this is the attachment")
                                                      .mediaType(MediaType.TEXT)
                                                      .attributes(new AttachmentAttributes("attachment"))
                                                      .build();

        MuleMessage message = MuleMessage.builder().payload(new MultiPartPayload(
                MuleMessage.builder().payload(TEST_PAYLOAD).build(),
                attachmentPart)).build();

        expected.expect(TransformerException.class);
        expected.expectMessage("\"MultiPartPayload\" cannot be transformed to [B.");

        getPayloadAsBytes(message);
    }
}
