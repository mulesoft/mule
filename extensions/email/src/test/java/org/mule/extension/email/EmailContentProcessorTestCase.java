/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.assertAttachmentContent;
import static org.mule.extension.email.util.EmailTestUtils.getMultipartTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getSinglePartTestMessage;
import org.mule.runtime.api.message.Message;
import org.mule.extension.email.internal.util.EmailContentProcessor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

import org.junit.Test;

public class EmailContentProcessorTestCase extends AbstractMuleTestCase {

  @Test
  public void emailTextBodyFromMultipart() throws Exception {
    javax.mail.Message message = getMultipartTestMessage();
    String messageBody = EmailContentProcessor.getInstance(message).getBody();
    assertThat(messageBody, is(EMAIL_CONTENT));
  }

  @Test
  public void emailTextBodyFromSinglePart() throws Exception {
    javax.mail.Message message = getSinglePartTestMessage();
    String messageBody = EmailContentProcessor.getInstance(message).getBody();
    assertThat(messageBody, is(EMAIL_CONTENT));
  }

  @Test
  public void emailAttachmentsFromMultipart() throws Exception {
    javax.mail.Message message = getMultipartTestMessage();
    List<Message> attachments = EmailContentProcessor.getInstance(message).getAttachments();
    assertThat(attachments, hasSize(2));
    assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT.getBytes());
    assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT.getBytes());
  }
}
