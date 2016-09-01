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
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import static org.mule.runtime.core.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.core.util.IOUtils.toMuleMessagePart;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
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

public class DefaultMultiPartPayloadTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void stringAttachment() throws Exception {
    final MuleMessage attachmentPart = MuleMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    MuleMessage message = MuleMessage.builder()
        .payload(new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    assertThat(((MultiPartPayload) message.getPayload()).getPartNames(), hasItem("attachment"));
    assertThat(((MultiPartPayload) message.getPayload()).getPart("attachment"), sameInstance(attachmentPart));
  }

  @Test
  public void fromUrlAttachment() throws Exception {
    final MuleMessage attachmentPart =
        toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);

    MuleMessage message = MuleMessage.builder()
        .payload(new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    assertThat(((MultiPartPayload) message.getPayload()).getParts(), hasSize(2));
    assertThat(((MultiPartPayload) message.getPayload()).getPartNames(), hasItem("spi-props"));
    assertThat(((MultiPartPayload) message.getPayload()).getPart("spi-props"), sameInstance(attachmentPart));

    assertThat(((MultiPartPayload) message.getPayload()).getPart("spi-props").getDataType().getMediaType(), is(MediaType.TEXT));
  }

  @Test
  public void xmlFromUrlAttachment() throws Exception {
    final MuleMessage attachmentPart1 =
        toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);
    final MuleMessage attachmentPart2 = toMuleMessagePart("dummy", getResourceAsUrl("dummy.xml", getClass()), null);

    MuleMessage message = MuleMessage.builder()
        .payload(new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart1, attachmentPart2))
        .build();

    assertThat(((MultiPartPayload) message.getPayload()).getParts(), hasSize(3));
    assertThat(((MultiPartPayload) message.getPayload()).getPart("dummy").getDataType().getMediaType(),
               is(MediaType.APPLICATION_XML));
  }

  @Test
  public void withBody() throws Exception {
    final MuleMessage attachmentPart = MuleMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    final MuleMessage bodyPart = MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build();

    MuleMessage message = MuleMessage.builder().payload(new DefaultMultiPartPayload(bodyPart, attachmentPart)).build();

    assertThat(((DefaultMultiPartPayload) message.getPayload()).hasBodyPart(), is(true));
    assertThat(((DefaultMultiPartPayload) message.getPayload()).getBodyPart(), sameInstance(bodyPart));
  }

  @Test
  public void withoutBody() throws Exception {
    final MuleMessage attachmentPart1 = MuleMessage.builder().payload("this is the attachment1").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final MuleMessage attachmentPart2 = MuleMessage.builder().payload("this is the attachment2").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    MuleMessage message = MuleMessage.builder().payload(new DefaultMultiPartPayload(attachmentPart1, attachmentPart2)).build();

    assertThat(((DefaultMultiPartPayload) message.getPayload()).hasBodyPart(), is(false));
  }

  @Test
  public void multiPartPayloadSerialization() throws Exception {
    final MuleMessage attachmentPart = MuleMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    MuleMessage message = MuleMessage.builder()
        .payload(new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    Flow flow = getTestFlow();
    setCurrentEvent(MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build());
    oos.writeObject(message);
    oos.flush();
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    message = (MuleMessage) ois.readObject();

    assertThat(((MultiPartPayload) message.getPayload()).getPartNames(), hasItem("attachment"));
    assertThat(((MultiPartPayload) message.getPayload()).getPart("attachment").getPayload(), is("this is the attachment"));
    assertThat(((MultiPartPayload) message.getPayload()).getPart("attachment").getAttributes(),
               equalTo(attachmentPart.getAttributes()));
  }

  @Test
  public void multiPartPayloadStreamsSerialization() throws Exception {
    final MuleMessage attachmentPart =
        toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT);
    assertThat(attachmentPart.getPayload(), instanceOf(InputStream.class));

    MuleMessage message = MuleMessage.builder()
        .payload(new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    Flow flow = getTestFlow();
    setCurrentEvent(MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build());
    oos.writeObject(message);
    oos.flush();
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    message = (MuleMessage) ois.readObject();

    assertThat(((MultiPartPayload) message.getPayload()).getPartNames(), hasItem("spi-props"));
    assertThat(((MultiPartPayload) message.getPayload()).getPart("spi-props").getPayload(), instanceOf(byte[].class));
    assertThat(((MultiPartPayload) message.getPayload()).getPart("spi-props").getAttributes(),
               equalTo(attachmentPart.getAttributes()));
  }

  @Test
  public void getPayloadAsStringFails() throws Exception {
    final MuleMessage attachmentPart = MuleMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    MuleMessage message = MuleMessage.builder()
        .payload(new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    expected.expect(TransformerException.class);
    expected.expectMessage("\"MultiPartPayload\" cannot be transformed to java.lang.String.");

    getPayloadAsString(message);
  }

  @Test
  public void getPayloadAsBytesFails() throws Exception {
    final MuleMessage attachmentPart = MuleMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    MuleMessage message = MuleMessage.builder()
        .payload(new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    expected.expect(TransformerException.class);
    expected.expectMessage("\"MultiPartPayload\" cannot be transformed to [B.");

    getPayloadAsBytes(message);
  }

  @Test
  public void nestedMultiPartPayloadsFlattens() throws Exception {
    final MuleMessage attachmentPart1 = MuleMessage.builder().payload("this is the attachment1").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final MuleMessage attachmentPart2 = MuleMessage.builder().payload("this is the attachment2").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    MuleMessage messageInner = MuleMessage.builder()
        .payload(new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart1))
        .attributes(BODY_ATTRIBUTES).build();

    MuleMessage message = MuleMessage.builder().payload(new DefaultMultiPartPayload(attachmentPart2, messageInner)).build();

    assertThat(((MultiPartPayload) message.getPayload()).getParts(), hasSize(3));
    assertThat(((MultiPartPayload) message.getPayload()).getPart("attachment1"), not(nullValue()));
    assertThat(((MultiPartPayload) message.getPayload()).getPart("attachment2"), not(nullValue()));
    assertThat(((DefaultMultiPartPayload) message.getPayload()).getBodyPart(), not(nullValue()));
  }

  @Test
  public void partWithInvalidAttributes() throws Exception {
    final MuleMessage attachmentPart = MuleMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    expected.expect(IllegalArgumentException.class);

    new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).build(), attachmentPart);
  }

}
