/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.api.EmailContentProcessor.process;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;

import javax.activation.DataHandler;
import javax.mail.Multipart;

import org.junit.Test;

public class ForwardTestCase extends SMTPTestCase {

  private static final String FORWARD_EMAIL = "forwardEmail";
  private static final String FORWARD_EMAIL_WITH_CONTENT = "forwardEmailWithContent";
  private static final String FORWARD_ADDING_ATTACHMENT = "forwardAddingAttachments";

  @Test
  public void forwardEmail() throws Exception {
    flowRunner(FORWARD_EMAIL).withPayload(EMAIL_CONTENT).withAttributes(getTestAttributes()).run();
    javax.mail.Message[] messages = getReceivedMessagesAndAssertCount(1);
    String body = process(messages[0]).getBody();
    assertBodyContent(body);
  }

  @Test
  public void forwardEmailWithContent() throws Exception {
    flowRunner(FORWARD_EMAIL_WITH_CONTENT).withPayload(EMAIL_CONTENT).withAttributes(getTestAttributes()).run();
    javax.mail.Message[] messages = getReceivedMessagesAndAssertCount(1);
    String body = process(messages[0]).getBody();
    assertThat(body, containsString("More Content To Forward"));
    assertThat(body, containsString(EMAIL_CONTENT));
  }

  @Test
  public void forwardAddingAttachment() throws Exception {

    flowRunner(FORWARD_ADDING_ATTACHMENT)
        .withPayload(EMAIL_CONTENT)
        .withAttributes(getTestAttributes()).run();

    javax.mail.Message repliedMessage = getReceivedMessagesAndAssertCount(1)[0];
    Multipart content = (Multipart) repliedMessage.getContent();

    DataHandler attachment = content.getBodyPart(1).getDataHandler();
    assertJsonAttachment(attachment);
  }
}
