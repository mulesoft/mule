/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import static java.nio.charset.Charset.availableCharsets;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import org.mule.runtime.core.util.IOUtils;

import org.junit.Test;

import java.io.InputStream;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Multipart;

public class SendTestCase extends SMTPTestCase {

  private static final String SEND_EMAIL = "sendEmail";
  private static final String SEND_EMAIL_CUSTOM_HEADERS = "sendEmailHeaders";
  private static final String SEND_EMAIL_WITH_ATTACHMENT = "sendEmailWithAttachment";
  private static final String SEND_ENCODED_MESSAGE = "sendEncodedMessage";
  private static final String SEND_EMAIL_WITHOUT_BODY = "sendEmailWithoutBody";

  @Test
  public void sendEmail() throws Exception {
    flowRunner(SEND_EMAIL).run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message sentMessage = messages[0];
    assertSubject(sentMessage.getSubject());
    assertBodyContent(sentMessage.getContent().toString().trim());
  }

  @Test
  public void sendEmailCustomHeaders() throws Exception {
    flowRunner(SEND_EMAIL_CUSTOM_HEADERS).run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message sentMessage = messages[0];
    assertSubject(sentMessage.getSubject());
    assertBodyContent(sentMessage.getContent().toString().trim());

    assertThat(sentMessage.getHeader("CustomOperationHeader"), arrayWithSize(1));
    assertThat(sentMessage.getHeader("CustomOperationHeader")[0], is("Dummy"));
  }

  @Test
  public void sendEmailWithAttachment() throws Exception {
    flowRunner(SEND_EMAIL_WITH_ATTACHMENT).run();
    Message[] messages = getReceivedMessagesAndAssertCount(4);
    for (Message message : messages) {
      Multipart content = (Multipart) message.getContent();
      assertThat(content.getCount(), is(4));

      Object body = content.getBodyPart(0).getContent();
      assertBodyContent((String) body);

      String textAttachment = (String) content.getBodyPart(1).getContent();
      assertThat(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, is(textAttachment));

      DataHandler jsonAttachment = content.getBodyPart(2).getDataHandler();
      assertJsonAttachment(jsonAttachment);

      DataHandler streamAttachment = content.getBodyPart(3).getDataHandler();
      assertThat(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, is(IOUtils.toString((InputStream) streamAttachment.getContent())));
    }
  }

  @Test
  public void sendEncodedMessage() throws Exception {
    final String defaultEncoding = muleContext.getConfiguration().getDefaultEncoding();
    assertThat(defaultEncoding, is(notNullValue()));

    final String customEncoding =
        availableCharsets().keySet().stream().filter(encoding -> !encoding.equals(defaultEncoding)).findFirst().orElse(null);

    assertThat(customEncoding, is(notNullValue()));

    flowRunner(SEND_ENCODED_MESSAGE).withPayload(WEIRD_CHAR_MESSAGE).withVariable("encoding", customEncoding).run();

    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Object content = ((String) messages[0].getContent()).trim();
    assertThat(content, is(new String(WEIRD_CHAR_MESSAGE.getBytes(customEncoding), customEncoding)));
  }

  @Test
  public void sendEmailWithoutBody() throws Exception {
    flowRunner(SEND_EMAIL_WITHOUT_BODY).run();
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message sentMessage = messages[0];
    assertSubject(sentMessage.getSubject());
    assertThat(sentMessage.getContent().toString().trim(), isEmptyString());
  }
}
