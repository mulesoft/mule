/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer.simple;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.activation.DataHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class AttachmentsMultiPartTransformersTestCase extends AbstractMuleContextTestCase {

  private AttachmentsToMultiPartTransformer a2mp = new AttachmentsToMultiPartTransformer();
  private MultiPartToAttachmentsTransformer mp2a = new MultiPartToAttachmentsTransformer();

  private Flow flow;
  private EventContext context;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void before() throws Exception {
    a2mp.setMuleContext(muleContext);
    mp2a.setMuleContext(muleContext);
    a2mp.initialise();
    mp2a.initialise();

    flow = getTestFlow(muleContext);
    context = DefaultEventContext.create(flow, TEST_CONNECTOR);
  }

  @After
  public void after() {
    a2mp.dispose();
    mp2a.dispose();
  }

  @Test
  public void nonMultiPartToAttachment() throws Exception {
    expected.expect(MessageTransformerException.class);
    mp2a.transform("", Event.builder(context).message(InternalMessage.of(TEST_PAYLOAD)).build());
  }

  @Test
  public void multiPartWithBody() throws MessageTransformerException {
    final InternalMessage attachmentPart = InternalMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    InternalMessage message = InternalMessage.builder()
        .payload(new DefaultMultiPartPayload(InternalMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    final InternalMessage response =
        (InternalMessage) mp2a.transform(message, Event.builder(context).message(message).flow(flow).build());

    assertThat(response.getOutboundAttachmentNames(), hasSize(1));
    assertThat(response.getOutboundAttachmentNames(), hasItem("attachment"));
    assertThat(response.getPayload().getValue(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void multiPartWithoutBody() throws MessageTransformerException {
    final InternalMessage attachment1Part = InternalMessage.builder().payload("this is the attachment1").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final InternalMessage attachment2Part = InternalMessage.builder().payload("this is the attachment2").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    InternalMessage message =
        InternalMessage.builder().payload(new DefaultMultiPartPayload(attachment1Part, attachment2Part)).build();

    final InternalMessage response =
        (InternalMessage) mp2a.transform(message, Event.builder(context).message(message).flow(flow).build());

    assertThat(response.getOutboundAttachmentNames(), hasSize(2));
    assertThat(response.getOutboundAttachmentNames(), hasItem("attachment1"));
    assertThat(response.getOutboundAttachmentNames(), hasItem("attachment2"));
    assertThat(response.getPayload().getValue(), nullValue());
  }

  @Test
  public void attachmentsWithPayload() throws MessageTransformerException {
    final DataHandler attDH = mock(DataHandler.class);
    when(attDH.getContentType()).thenReturn(MediaType.TEXT.toRfcString());
    final InternalMessage message =
        InternalMessage.builder().payload(TEST_PAYLOAD).addInboundAttachment("attachment", attDH).build();

    final InternalMessage response =
        (InternalMessage) a2mp.transform(message, Event.builder(context).message(message).flow(flow).build());

    assertThat(response.getPayload().getValue(), instanceOf(MultiPartPayload.class));
    assertThat(((MultiPartPayload) response.getPayload().getValue()).getParts(), hasSize(2));
    assertThat(((DefaultMultiPartPayload) response.getPayload().getValue()).hasBodyPart(), is(true));
    assertThat(((MultiPartPayload) response.getPayload().getValue()).getPartNames(), hasItem("attachment"));
  }

  @Test
  public void attachmentsWithoutPayload() throws MessageTransformerException {
    final DataHandler attDH = mock(DataHandler.class);
    when(attDH.getContentType()).thenReturn(MediaType.TEXT.toRfcString());
    final InternalMessage message = InternalMessage.builder().nullPayload().addInboundAttachment("attachment", attDH).build();

    final InternalMessage response =
        (InternalMessage) a2mp.transform(message, Event.builder(context).message(message).flow(flow).build());

    assertThat(response.getPayload().getValue(), instanceOf(MultiPartPayload.class));
    assertThat(((MultiPartPayload) response.getPayload().getValue()).getParts(), hasSize(1));
    assertThat(((DefaultMultiPartPayload) response.getPayload().getValue()).hasBodyPart(), is(false));
    assertThat(((MultiPartPayload) response.getPayload().getValue()).getPartNames(), hasItem("attachment"));
  }

}
