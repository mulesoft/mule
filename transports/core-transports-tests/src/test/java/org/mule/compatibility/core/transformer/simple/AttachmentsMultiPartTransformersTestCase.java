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

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMessageExecutionContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerMessagingException;
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

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Before
  public void before() throws InitialisationException {
    a2mp.setMuleContext(muleContext);
    mp2a.setMuleContext(muleContext);
    a2mp.initialise();
    mp2a.initialise();
  }

  @After
  public void after() {
    a2mp.dispose();
    mp2a.dispose();
  }

  @Test
  public void nonMultiPartPayloadToAttachment() throws Exception {
    expected.expect(TransformerMessagingException.class);
    mp2a.transform("", new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null),
                                            getTestMuleMessage(), getTestFlow()));
  }

  @Test
  public void multiPartWithBody() throws TransformerMessagingException, Exception {
    final MuleMessage attachmentPart = MuleMessage.builder().payload("this is the attachment").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment")).build();

    MuleMessage message = MuleMessage.builder()
        .payload(new DefaultMultiPartPayload(MuleMessage.builder().payload(TEST_PAYLOAD).attributes(BODY_ATTRIBUTES).build(),
                                             attachmentPart))
        .build();

    final MuleMessage response = (MuleMessage) mp2a
        .transform(message, new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null),
                                                 message, getTestFlow()));

    assertThat(response.getOutboundAttachmentNames(), hasSize(1));
    assertThat(response.getOutboundAttachmentNames(), hasItem("attachment"));
    assertThat(response.getPayload(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void multiPartWithoutBody() throws TransformerMessagingException, Exception {
    final MuleMessage attachment1Part = MuleMessage.builder().payload("this is the attachment1").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment1")).build();
    final MuleMessage attachment2Part = MuleMessage.builder().payload("this is the attachment2").mediaType(MediaType.TEXT)
        .attributes(new PartAttributes("attachment2")).build();

    MuleMessage message = MuleMessage.builder().payload(new DefaultMultiPartPayload(attachment1Part, attachment2Part)).build();

    final MuleMessage response = (MuleMessage) mp2a
        .transform(message, new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null),
                                                 message, getTestFlow()));

    assertThat(response.getOutboundAttachmentNames(), hasSize(2));
    assertThat(response.getOutboundAttachmentNames(), hasItem("attachment1"));
    assertThat(response.getOutboundAttachmentNames(), hasItem("attachment2"));
    assertThat(response.getPayload(), nullValue());
  }

  @Test
  public void attachmentsWithPayload() throws TransformerMessagingException, Exception {
    final DataHandler attDH = mock(DataHandler.class);
    when(attDH.getContentType()).thenReturn(MediaType.TEXT.toRfcString());
    final MuleMessage message = MuleMessage.builder().payload(TEST_PAYLOAD).addInboundAttachment("attachment", attDH).build();

    final MuleMessage response = (MuleMessage) a2mp
        .transform(message, new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null),
                                                 message, getTestFlow()));

    assertThat(response.getPayload(), instanceOf(MultiPartPayload.class));
    assertThat(((MultiPartPayload) response.getPayload()).getParts(), hasSize(2));
    assertThat(((DefaultMultiPartPayload) response.getPayload()).hasBodyPart(), is(true));
    assertThat(((MultiPartPayload) response.getPayload()).getPartNames(), hasItem("attachment"));
  }

  @Test
  public void attachmentsWithoutPayload() throws TransformerMessagingException, Exception {
    final DataHandler attDH = mock(DataHandler.class);
    when(attDH.getContentType()).thenReturn(MediaType.TEXT.toRfcString());
    final MuleMessage message = MuleMessage.builder().nullPayload().addInboundAttachment("attachment", attDH).build();

    final MuleMessage response = (MuleMessage) a2mp
        .transform(message, new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null),
                                                 message, getTestFlow()));

    assertThat(response.getPayload(), instanceOf(MultiPartPayload.class));
    assertThat(((MultiPartPayload) response.getPayload()).getParts(), hasSize(1));
    assertThat(((DefaultMultiPartPayload) response.getPayload()).hasBodyPart(), is(false));
    assertThat(((MultiPartPayload) response.getPayload()).getPartNames(), hasItem("attachment"));
  }

}
