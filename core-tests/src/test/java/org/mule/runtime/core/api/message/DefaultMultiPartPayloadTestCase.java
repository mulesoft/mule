/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.message;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.core.api.util.IOUtils.toMuleMessagePart;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class DefaultMultiPartPayloadTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public void stringAttachment() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(TEXT)
        .attributes(new PartAttributes("attachment")).build();

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    final MultiPartPayload multiPartPayload = (MultiPartPayload) message.getPayload().getValue();
    assertThat(multiPartPayload.getPartNames(), hasItem("attachment"));
    assertThat(multiPartPayload.getPart("attachment"), sameInstance(attachmentPart));

    assertThat(multiPartPayload.getNamedParts().entrySet(), hasSize(2));
    assertThat(multiPartPayload.getNamedParts(), hasEntry(is("attachment"), sameInstance(attachmentPart)));
    assertThat(multiPartPayload.getNamedParts(), hasKey("_body"));
  }

  @Test
  public void fromUrlAttachment() throws Exception {
    final Message attachmentPart = toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), TEXT);

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    final MultiPartPayload multiPartPayload = (MultiPartPayload) message.getPayload().getValue();
    assertThat(multiPartPayload.getParts(), hasSize(2));
    assertThat(multiPartPayload.getPartNames(), hasItem("spi-props"));
    assertThat(multiPartPayload.getPart("spi-props"), sameInstance(attachmentPart));

    assertThat(multiPartPayload.getPart("spi-props").getPayload().getDataType().getMediaType(), is(TEXT));

    assertThat(multiPartPayload.getNamedParts().entrySet(), hasSize(2));
    assertThat(multiPartPayload.getNamedParts(), hasEntry(is("spi-props"), sameInstance(attachmentPart)));
    assertThat(multiPartPayload.getNamedParts(), hasKey("_body"));
  }

  @Test
  public void xmlFromUrlAttachment() throws Exception {
    final Message attachmentPart1 = toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), TEXT);
    final Message attachmentPart2 = toMuleMessagePart("dummy", getResourceAsUrl("dummy.xml", getClass()), null);

    Message message = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart1, attachmentPart2))
        .build();

    final MultiPartPayload multiPartPayload = (MultiPartPayload) message.getPayload().getValue();
    assertThat(multiPartPayload.getParts(), hasSize(3));
    assertThat(multiPartPayload.getPart("dummy").getPayload().getDataType().getMediaType(), is(APPLICATION_XML));

    assertThat(multiPartPayload.getNamedParts().entrySet(), hasSize(3));
    assertThat(multiPartPayload.getNamedParts(), hasEntry(is("spi-props"), sameInstance(attachmentPart1)));
    assertThat(multiPartPayload.getNamedParts(), hasEntry(is("dummy"), sameInstance(attachmentPart2)));
    assertThat(multiPartPayload.getNamedParts(), hasKey("_body"));
  }

  @Test
  public void withBody() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(TEXT)
        .attributes(new PartAttributes("attachment")).build();

    final Message bodyPart = Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build();

    Message message = of(new DefaultMultiPartPayload(bodyPart, attachmentPart));

    final DefaultMultiPartPayload multiPartPayload = (DefaultMultiPartPayload) message.getPayload().getValue();
    assertThat(multiPartPayload.hasBodyPart(), is(true));
    assertThat(multiPartPayload.getBodyPart(), sameInstance(bodyPart));

    assertThat(multiPartPayload.getNamedParts().entrySet(), hasSize(2));
    assertThat(multiPartPayload.getNamedParts(), hasEntry(is("attachment"), sameInstance(attachmentPart)));
    assertThat(multiPartPayload.getNamedParts(), hasKey("_body"));
  }

  @Test
  public void withoutBody() throws Exception {
    final Message attachmentPart1 = Message.builder().payload("this is the attachment1").mediaType(TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final Message attachmentPart2 = Message.builder().payload("this is the attachment2").mediaType(TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    Message message = of(new DefaultMultiPartPayload(attachmentPart1, attachmentPart2));

    final DefaultMultiPartPayload multiPartPayload = (DefaultMultiPartPayload) message.getPayload().getValue();
    assertThat(multiPartPayload.hasBodyPart(), is(false));

    assertThat(multiPartPayload.getNamedParts().entrySet(), hasSize(2));
    assertThat(multiPartPayload.getNamedParts(), hasEntry(is("attachment1"), sameInstance(attachmentPart1)));
    assertThat(multiPartPayload.getNamedParts(), hasEntry(is("attachment2"), sameInstance(attachmentPart2)));
    assertThat(multiPartPayload.getNamedParts(), not(hasKey("_body")));
  }

  @Test
  public void multiPartPayloadSerialization() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(TEXT)
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

    final MultiPartPayload multiPartPayload = (MultiPartPayload) message.getPayload().getValue();
    assertThat(multiPartPayload.getPartNames(), hasItem("attachment"));
    assertThat(multiPartPayload.getPart("attachment").getPayload().getValue(), is("this is the attachment"));
    assertThat(multiPartPayload.getPart("attachment").getAttributes().getValue(),
               equalTo(attachmentPart.getAttributes().getValue()));

    assertThat(multiPartPayload.getNamedParts().entrySet(), hasSize(2));
    assertThat(multiPartPayload.getNamedParts(), hasKey("attachment"));
    assertThat(multiPartPayload.getNamedParts(), hasKey("_body"));
  }

  @Test
  public void multiPartPayloadStreamsSerialization() throws Exception {
    final Message attachmentPart = toMuleMessagePart("spi-props", getResourceAsUrl("test-spi.properties", getClass()), TEXT);
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

    final MultiPartPayload multiPartPayload = (MultiPartPayload) message.getPayload().getValue();
    assertThat(multiPartPayload.getPartNames(), hasItem("spi-props"));
    assertThat(multiPartPayload.getPart("spi-props").getPayload().getValue(), instanceOf(byte[].class));
    assertThat(multiPartPayload.getPart("spi-props").getAttributes().getValue(),
               equalTo(attachmentPart.getAttributes().getValue()));

    assertThat(multiPartPayload.getNamedParts().entrySet(), hasSize(2));
    assertThat(multiPartPayload.getNamedParts(), hasKey("spi-props"));
    assertThat(multiPartPayload.getNamedParts(), hasKey("_body"));
  }

  @Test
  public void getPayloadAsStringFails() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(TEXT)
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
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(TEXT)
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
    final Message attachmentPart1 = Message.builder().payload("this is the attachment1").mediaType(TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final Message attachmentPart2 = Message.builder().payload("this is the attachment2").mediaType(TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    Message messageInner = Message.builder()
        .payload(new DefaultMultiPartPayload(Message.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart1))
        .attributes(BODY_ATTRIBUTES).build();

    Message message = Message.builder().payload(new DefaultMultiPartPayload(attachmentPart2, messageInner)).build();

    final MultiPartPayload multiPartPayload = (MultiPartPayload) message.getPayload().getValue();
    assertThat(multiPartPayload.getParts(), hasSize(3));
    assertThat(multiPartPayload.getPart("attachment1"), not(nullValue()));
    assertThat(multiPartPayload.getPart("attachment2"), not(nullValue()));
    assertThat(((DefaultMultiPartPayload) multiPartPayload).getBodyPart(), not(nullValue()));

    assertThat(multiPartPayload.getNamedParts().entrySet(), hasSize(3));
    assertThat(multiPartPayload.getNamedParts(), hasEntry(is("attachment1"), sameInstance(attachmentPart1)));
    assertThat(multiPartPayload.getNamedParts(), hasEntry(is("attachment2"), sameInstance(attachmentPart2)));
    assertThat(multiPartPayload.getNamedParts(), hasKey("_body"));
  }

  @Test
  public void partWithInvalidAttributes() throws Exception {
    final Message attachmentPart = Message.builder().payload("this is the attachment").mediaType(TEXT)
        .attributes(new PartAttributes("attachment")).build();

    expected.expect(IllegalArgumentException.class);

    new DefaultMultiPartPayload(of(TEST_PAYLOAD), attachmentPart);
  }

}
