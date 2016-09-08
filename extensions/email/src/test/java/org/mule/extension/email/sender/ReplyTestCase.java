/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.mockito.Mockito.when;
import static org.mule.extension.email.internal.commands.ReplyCommand.IN_REPLY_TO_HEADER;
import static org.mule.extension.email.internal.commands.ReplyCommand.NO_EMAIL_FOUND;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.MG_EMAIL;
import static org.mule.runtime.core.api.MuleMessage.builder;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import static org.mule.runtime.core.util.CollectionUtils.singletonList;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;

import org.junit.Test;
import org.mockito.internal.matchers.StartsWith;


public class ReplyTestCase extends SMTPTestCase {

  private static final String REPLY_EMAIL = "replyEmail";
  private static final String REPLY_ADDING_ATTACHMENT = "replyAddingAttachment";
  private static final String REPLY_EMAIL_WITH_ATTACHMENTS = "replyEmailWithAttachments";

  @Test
  public void replyEmail() throws Exception {
    EmailAttributes attributes = getTestAttributes();
    when(attributes.getReplyToAddresses()).thenReturn(singletonList(MG_EMAIL));

    flowRunner(REPLY_EMAIL).withPayload(EMAIL_CONTENT).withAttributes(attributes).run();

    Message repliedMessage = getReceivedMessagesAndAssertCount(1)[0];
    String subject = repliedMessage.getSubject();
    Address[] recipients = repliedMessage.getAllRecipients();
    String inReplyHeaderValue = repliedMessage.getHeader(IN_REPLY_TO_HEADER)[0];

    assertThat(subject, new StartsWith("Re"));
    assertThat(recipients, arrayWithSize(1));
    assertThat(recipients[0].toString(), is(MG_EMAIL));
    assertThat(inReplyHeaderValue, is(Integer.toString(attributes.getId())));
  }

  @Test
  public void failReply() throws Exception {
    expectedException.expectCause(isA(EmailException.class));
    expectedException.expectMessage(containsString(NO_EMAIL_FOUND));
    flowRunner(REPLY_EMAIL).run();
  }

  @Test
  public void replyAttachment() throws Exception {
    EmailAttributes attributes = getTestAttributes();
    when(attributes.getReplyToAddresses()).thenReturn(singletonList(MG_EMAIL));

    MuleMessage contentPart = builder().payload(EMAIL_CONTENT).attributes(BODY_ATTRIBUTES).build();
    MuleMessage attachmentPart = builder().payload(EMAIL_JSON_ATTACHMENT_CONTENT)
        .attributes(new PartAttributes("json", "attachment.json", 123, emptyMap())).build();

    flowRunner(REPLY_EMAIL_WITH_ATTACHMENTS)
        .withPayload(new DefaultMultiPartPayload(contentPart, attachmentPart))
        .withAttributes(attributes).run();

    Message repliedMessage = getReceivedMessagesAndAssertCount(1)[0];
    Multipart content = (Multipart) repliedMessage.getContent();
    assertJsonAttachment(content.getBodyPart(1).getDataHandler());
  }

  @Test
  public void replyAddingAttachment() throws Exception {
    EmailAttributes attributes = getTestAttributes();
    when(attributes.getReplyToAddresses()).thenReturn(singletonList(MG_EMAIL));

    flowRunner(REPLY_ADDING_ATTACHMENT)
        .withPayload(EMAIL_CONTENT)
        .withAttributes(attributes).run();

    Message repliedMessage = getReceivedMessagesAndAssertCount(1)[0];
    Multipart content = (Multipart) repliedMessage.getContent();

    Object body = content.getBodyPart(0).getContent();
    assertThat(body, is("Reply " + EMAIL_CONTENT));

    DataHandler attachment = content.getBodyPart(1).getDataHandler();
    assertJsonAttachment(attachment);
  }
}
