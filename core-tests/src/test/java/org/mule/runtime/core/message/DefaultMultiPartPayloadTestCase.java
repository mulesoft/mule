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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import static org.mule.runtime.core.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.core.util.IOUtils.toMuleMessagePart;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.message.InternalMessage;
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

public class DefaultMultiPartPayloadTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void stringAttachment() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPartNames(), hasItem("attachment"));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("attachment"), sameInstance(attachmentPart));
  }

  @Test
  public void fromUrlAttachment() throws Exception {
    final Message attachmentPart =
        toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    assertThat(((MultiPartPayload) message.getPayload().getValue()).getParts(), hasSize(2));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPartNames(), hasItem("spi-props"));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("spi-props"), sameInstance(attachmentPart));

    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("spi-props").getPayload().getDataType()
        .getMediaType(), is(MediaType.TEXT));
  }

  @Test
  public void xmlFromUrlAttachment() throws Exception {
    final Message attachmentPart1 =
        toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);
    final Message attachmentPart2 = toMuleMessagePart("dummy", getResourceAsUrl("dummy.xml", getClass()), null);

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart1, attachmentPart2))
        .build();

    assertThat(((MultiPartPayload) message.getPayload().getValue()).getParts(), hasSize(3));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("dummy").getPayload().getDataType().getMediaType(),
               is(MediaType.APPLICATION_XML));
  }

  @Test
  public void withBody() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    final Message bodyPart = Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build();

    Message message = of(new DefaultMultiPartPayload(bodyPart, attachmentPart));

    assertThat(((DefaultMultiPartPayload) message.getPayload().getValue()).hasBodyPart(), is(true));
    assertThat(((DefaultMultiPartPayload) message.getPayload().getValue()).getBodyPart(), sameInstance(bodyPart));
  }

  @Test
  public void withoutBody() throws Exception {
    final Message attachmentPart1 = Message.builder().payload("this is the attachment1").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final Message attachmentPart2 = Message.builder().payload("this is the attachment2").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    Message message = of(new DefaultMultiPartPayload(attachmentPart1, attachmentPart2));

    assertThat(((DefaultMultiPartPayload) message.getPayload().getValue()).hasBodyPart(), is(false));
  }

  @Test
  public void multiPartPayloadSerialization() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    setCurrentEvent(eventBuilder().message(message).build());
    oos.writeObject(message);
    oos.flush();
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    message = (InternalMessage) ois.readObject();

    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPartNames(), hasItem("attachment"));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("attachment").getPayload().getValue(),
               is("this is the attachment"));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("attachment").getAttributes(),
               equalTo(attachmentPart.getAttributes()));
  }

  @Test
  public void multiPartPayloadStreamsSerialization() throws Exception {
    final Message attachmentPart =
        toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);
    assertThat(attachmentPart.getPayload().getValue(), instanceOf(InputStream.class));

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    setCurrentEvent(eventBuilder().message(message).build());
    oos.writeObject(message);
    oos.flush();
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    message = (InternalMessage) ois.readObject();

    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPartNames(), hasItem("spi-props"));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("spi-props").getPayload().getValue(),
               instanceOf(byte[].class));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("spi-props").getAttributes(),
               equalTo(attachmentPart.getAttributes()));
  }

  @Test
  public void getPayloadAsStringFails() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    expected.expect(TransformerException.class);
    expected.expectMessage("\"MultiPartPayload\" cannot be transformed to java.lang.String.");

    getPayloadAsString(message);
  }

  @Test
  public void getPayloadAsBytesFails() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    expected.expect(TransformerException.class);
    expected.expectMessage("\"MultiPartPayload\" cannot be transformed to [B.");

    getPayloadAsBytes(message);
  }

  @Test
  public void nestedMultiPartFlattens() throws Exception {
    final Message attachmentPart1 = Message.builder().payload("this is the attachment1").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final Message attachmentPart2 = Message.builder().payload("this is the attachment2").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    Message messageInner = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart1))
        .attributes(BODY_ATTRIBUTES).build();

    Message message = Message.builder().payload(new DefaultMultiPartPayload(attachmentPart2, messageInner)).build();

    assertThat(((MultiPartPayload) message.getPayload().getValue()).getParts(), hasSize(3));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("attachment1"), not(nullValue()));
    assertThat(((MultiPartPayload) message.getPayload().getValue()).getPart("attachment2"), not(nullValue()));
    assertThat(((DefaultMultiPartPayload) message.getPayload().getValue()).getBodyPart(), not(nullValue()));
  }

  @Test
  public void partWithInvalidAttributes() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    expected.expect(IllegalArgumentException.class);

    new DefaultMultiPartPayload(of(TEST_PAYLOAD), attachmentPart);
  }

}
