/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.message.NullAttributes.NULL_ATTRIBUTES;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class DefaultMuleMessageAttachmentsTestCase extends AbstractMuleContextTestCase {

  private static final String ATTACHMENT_KEY = "attachmentKey";
  private static final String ATTACHMENT_VALUE = "attachmentValue";
  private Attributes testAttributes = NULL_ATTRIBUTES;

  @Test
  public void inboundAttachmentMap() {
    final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
    Map<String, DataHandler> inboundAttachments = singletonMap(ATTACHMENT_KEY, attachmentValue);
    MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).inboundAttachments(inboundAttachments).build();

    assertThat(message.getInboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
    assertThat(message.getInboundAttachmentNames(), hasSize(1));
    assertThat(message.getInboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
  }

  @Test
  public void inboundAttachmentMapCopy() {
    final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
    Map<String, DataHandler> inboundAttachments = singletonMap(ATTACHMENT_KEY, attachmentValue);
    MuleMessage copy = new DefaultMuleMessageBuilder(new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD)
        .inboundAttachments(inboundAttachments).build()).build();

    assertThat(copy.getInboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
    assertThat(copy.getInboundAttachmentNames(), hasSize(1));
    assertThat(copy.getInboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
  }

  @Test
  public void outboundAttachmentMap() {
    final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
    Map<String, DataHandler> outboundAttachments = singletonMap(ATTACHMENT_KEY, attachmentValue);
    MuleMessage message = new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).outboundAttachments(outboundAttachments).build();

    assertThat(message.getOutboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
    assertThat(message.getOutboundAttachmentNames(), hasSize(1));
    assertThat(message.getOutboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
  }

  @Test
  public void outboundAttachmentMapCopy() {
    final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
    Map<String, DataHandler> outboundAttachments = singletonMap(ATTACHMENT_KEY, attachmentValue);
    MuleMessage copy = new DefaultMuleMessageBuilder(new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD)
        .outboundAttachments(outboundAttachments).build()).build();

    assertThat(copy.getOutboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
    assertThat(copy.getOutboundAttachmentNames(), hasSize(1));
    assertThat(copy.getOutboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
  }

  @Test
  public void inboundAttachment() {
    final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
    MuleMessage message =
        new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).addInboundAttachment(ATTACHMENT_KEY, attachmentValue).build();

    assertThat(message.getInboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
    assertThat(message.getInboundAttachmentNames(), hasSize(1));
    assertThat(message.getInboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
  }

  @Test
  public void outboundAttachment() {
    final DataHandler attachmentValue = new DataHandler(ATTACHMENT_VALUE, TEXT.toString());
    MuleMessage message =
        new DefaultMuleMessageBuilder().payload(TEST_PAYLOAD).addOutboundAttachment(ATTACHMENT_KEY, attachmentValue).build();

    assertThat(message.getOutboundAttachment(ATTACHMENT_KEY), equalTo(attachmentValue));
    assertThat(message.getOutboundAttachmentNames(), hasSize(1));
    assertThat(message.getOutboundAttachmentNames(), hasItem(ATTACHMENT_KEY));
  }

  //
  // attachments
  //
  @Test
  public void testLegacyAddingAttachment() throws Exception {
    DataHandler handler = new DataHandler("this is the attachment", "text/plain");
    MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).addOutboundAttachment("attachment", handler).build();

    assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
    assertEquals(handler, message.getOutboundAttachment("attachment"));
  }

  @Test
  public void testAddingOutboundAttachment() throws Exception {
    DataHandler handler = new DataHandler("this is the attachment", "text/plain");
    MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).addOutboundAttachment("attachment", handler).build();

    assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
    assertEquals(handler, message.getOutboundAttachment("attachment"));
    assertEquals(0, message.getInboundAttachmentNames().size());

    message = MuleMessage.builder(message).removeOutboundAttachment("attachment").build();
    assertEquals(0, message.getOutboundAttachmentNames().size());

    message = MuleMessage.builder(message).addOutboundAttachment("spi-props", IOUtils
        .toDataHandler("spi-props", IOUtils.getResourceAsUrl("test-spi.properties", getClass()), MediaType.TEXT)).build();


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
  public void testAddingInboundAttachment() throws Exception {
    Map<String, DataHandler> attachments = new HashMap<>();

    String attachmentData = "this is the attachment";
    DataHandler dh = new DataHandler(attachmentData, "text/plain");
    attachments.put("attachment", dh);
    MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).inboundAttachments(attachments).build();

    assertTrue(message.getInboundAttachmentNames().contains("attachment"));
    assertEquals(dh, message.getInboundAttachment("attachment"));
    assertEquals(0, message.getOutboundAttachmentNames().size());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    Flow flow = getTestFlow();
    setCurrentEvent(MuleEvent.builder(DefaultMessageContext.create(flow, TEST_CONNECTOR)).message(message).flow(flow).build());
    oos.writeObject(message);
    oos.flush();
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    MuleMessage message2 = (MuleMessage) ois.readObject();
    assertTrue(message2.getInboundAttachmentNames().contains("attachment"));
    assertEquals(message2.getInboundAttachment("attachment").getContent(), attachmentData);
    assertEquals(0, message2.getOutboundAttachmentNames().size());

  }

  @Test
  public void testNewMuleMessageFromMuleMessageWithAttachment() throws Exception {
    MuleMessage previous = createMuleMessage();

    DataHandler handler = new DataHandler("this is the attachment", "text/plain");
    MuleMessage message =
        MuleMessage.builder(previous).payload(TEST_MESSAGE).addOutboundAttachment("attachment", handler).build();

    assertTrue(message.getOutboundAttachmentNames().contains("attachment"));
    assertEquals(handler, message.getOutboundAttachment("attachment"));
  }

  private MuleMessage createMuleMessage() {
    return MuleMessage.builder().payload(TEST_PAYLOAD).attributes(testAttributes)
        .addOutboundProperty("MuleMessage", "MuleMessage").build();
  }

}
