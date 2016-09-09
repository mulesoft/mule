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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.message.DefaultEventBuilder.MuleEventImplementation.setCurrentEvent;
import static org.mule.runtime.core.message.DefaultMultiPartContent.BODY_ATTRIBUTES;
import static org.mule.runtime.core.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.core.util.IOUtils.toMuleMessagePart;

import org.mule.runtime.api.message.MultiPartContent;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.construct.Flow;
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

public class DefaultMultiPartContentTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void stringAttachment() throws Exception {
    final InternalMessage attachmentPart = InternalMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    InternalMessage message = InternalMessage.builder()
        .payload(new DefaultMultiPartContent(InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    assertThat(((MultiPartContent) message.getPayload()).getPartNames(), hasItem("attachment"));
    assertThat(((MultiPartContent) message.getPayload()).getPart("attachment"), sameInstance(attachmentPart));
  }

  @Test
  public void fromUrlAttachment() throws Exception {
    final InternalMessage attachmentPart =
        toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);

    InternalMessage message = InternalMessage.builder()
        .payload(new DefaultMultiPartContent(InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    assertThat(((MultiPartContent) message.getPayload()).getParts(), hasSize(2));
    assertThat(((MultiPartContent) message.getPayload()).getPartNames(), hasItem("spi-props"));
    assertThat(((MultiPartContent) message.getPayload()).getPart("spi-props"), sameInstance(attachmentPart));

    assertThat(((MultiPartContent) message.getPayload()).getPart("spi-props").getDataType().getMediaType(), is(MediaType.TEXT));
  }

  @Test
  public void xmlFromUrlAttachment() throws Exception {
    final InternalMessage attachmentPart1 =
        toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);
    final InternalMessage attachmentPart2 = toMuleMessagePart("dummy", getResourceAsUrl("dummy.xml", getClass()), null);

    InternalMessage message = InternalMessage.builder()
        .payload(new DefaultMultiPartContent(InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart1, attachmentPart2))
        .build();

    assertThat(((MultiPartContent) message.getPayload()).getParts(), hasSize(3));
    assertThat(((MultiPartContent) message.getPayload()).getPart("dummy").getDataType().getMediaType(),
               is(MediaType.APPLICATION_XML));
  }

  @Test
  public void withBody() throws Exception {
    final InternalMessage attachmentPart = InternalMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    final InternalMessage bodyPart = InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build();

    InternalMessage message = InternalMessage.builder().payload(new DefaultMultiPartContent(bodyPart, attachmentPart)).build();

    assertThat(((DefaultMultiPartContent) message.getPayload()).hasBodyPart(), is(true));
    assertThat(((DefaultMultiPartContent) message.getPayload()).getBodyPart(), sameInstance(bodyPart));
  }

  @Test
  public void withoutBody() throws Exception {
    final InternalMessage attachmentPart1 = InternalMessage.builder().payload("this is the attachment1").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final InternalMessage attachmentPart2 = InternalMessage.builder().payload("this is the attachment2").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    InternalMessage message =
        InternalMessage.builder().payload(new DefaultMultiPartContent(attachmentPart1, attachmentPart2)).build();

    assertThat(((DefaultMultiPartContent) message.getPayload()).hasBodyPart(), is(false));
  }

  @Test
  public void multiPartPayloadSerialization() throws Exception {
    final InternalMessage attachmentPart = InternalMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    InternalMessage message = InternalMessage.builder()
        .payload(new DefaultMultiPartContent(InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    Flow flow = getTestFlow();
    setCurrentEvent(Event.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build());
    oos.writeObject(message);
    oos.flush();
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    message = (InternalMessage) ois.readObject();

    assertThat(((MultiPartContent) message.getPayload()).getPartNames(), hasItem("attachment"));
    assertThat(((MultiPartContent) message.getPayload()).getPart("attachment").getPayload(), is("this is the attachment"));
    assertThat(((MultiPartContent) message.getPayload()).getPart("attachment").getAttributes(),
               equalTo(attachmentPart.getAttributes()));
  }

  @Test
  public void multiPartPayloadStreamsSerialization() throws Exception {
    final InternalMessage attachmentPart =
        toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);
    assertThat(attachmentPart.getPayload(), instanceOf(InputStream.class));

    InternalMessage message = InternalMessage.builder()
        .payload(new DefaultMultiPartContent(InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    Flow flow = getTestFlow();
    setCurrentEvent(Event.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build());
    oos.writeObject(message);
    oos.flush();
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    message = (InternalMessage) ois.readObject();

    assertThat(((MultiPartContent) message.getPayload()).getPartNames(), hasItem("spi-props"));
    assertThat(((MultiPartContent) message.getPayload()).getPart("spi-props").getPayload(), instanceOf(byte[].class));
    assertThat(((MultiPartContent) message.getPayload()).getPart("spi-props").getAttributes(),
               equalTo(attachmentPart.getAttributes()));
  }

  @Test
  public void getPayloadAsStringFails() throws Exception {
    final InternalMessage attachmentPart = InternalMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    InternalMessage message = InternalMessage.builder()
        .payload(new DefaultMultiPartContent(InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    expected.expect(TransformerException.class);
    expected.expectMessage("\"MultiPartContent\" cannot be transformed to java.lang.String.");

    getPayloadAsString(message);
  }

  @Test
  public void getPayloadAsBytesFails() throws Exception {
    final InternalMessage attachmentPart = InternalMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    InternalMessage message = InternalMessage.builder()
        .payload(new DefaultMultiPartContent(InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    expected.expect(TransformerException.class);
    expected.expectMessage("\"MultiPartContent\" cannot be transformed to [B.");

    getPayloadAsBytes(message);
  }

  @Test
  public void nestedMultiPartFlattens() throws Exception {
    final InternalMessage attachmentPart1 = InternalMessage.builder().payload("this is the attachment1").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final InternalMessage attachmentPart2 = InternalMessage.builder().payload("this is the attachment2").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    InternalMessage messageInner = InternalMessage.builder()
        .payload(new DefaultMultiPartContent(InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart1))
        .attributes(BODY_ATTRIBUTES).build();

    InternalMessage message =
        InternalMessage.builder().payload(new DefaultMultiPartContent(attachmentPart2, messageInner)).build();

    assertThat(((MultiPartContent) message.getPayload()).getParts(), hasSize(3));
    assertThat(((MultiPartContent) message.getPayload()).getPart("attachment1"), not(nullValue()));
    assertThat(((MultiPartContent) message.getPayload()).getPart("attachment2"), not(nullValue()));
    assertThat(((DefaultMultiPartContent) message.getPayload()).getBodyPart(), not(nullValue()));
  }

  @Test
  public void partWithInvalidAttributes() throws Exception {
    final InternalMessage attachmentPart = InternalMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    expected.expect(IllegalArgumentException.class);

    new DefaultMultiPartContent(InternalMessage.builder().payload(TEST_PAYLOAD).build(), attachmentPart);
  }

}
