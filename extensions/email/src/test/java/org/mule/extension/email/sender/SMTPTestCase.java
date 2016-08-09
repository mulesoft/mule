/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import static java.lang.String.format;
import static java.nio.charset.Charset.availableCharsets;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.email.api.EmailContentProcessor.process;
import static org.mule.extension.email.internal.commands.ReplyCommand.IN_REPLY_TO_HEADER;
import static org.mule.extension.email.internal.commands.ReplyCommand.NO_EMAIL_FOUND;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.MG_EMAIL;
import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.internal.matchers.StartsWith;

@RunnerDelegateTo(Parameterized.class)
public class SMTPTestCase extends EmailConnectorTestCase {

  private static final String WEIRD_CHAR_MESSAGE = "This is a messag\u00ea with weird chars \u00f1.";

  private static final String SEND_EMAIL = "sendEmail";
  private static final String SEND_EMAIL_WITH_ATTACHMENT = "sendEmailWithAttachment";
  private static final String FORWARD_EMAIL = "forwardEmail";
  private static final String FORWARD_EMAIL_WITH_CONTENT = "forwardEmailWithContent";
  private static final String REPLY_EMAIL = "replyEmail";
  private static final String SEND_ENCODED_MESSAGE = "sendEncodedMessage";

  @Parameter
  public String protocol;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"smtp"}, {"smtps"}});
  }

  @Override
  protected String getConfigFile() {
    return format("sender/%s.xml", protocol);
  }

  @Override
  public String getProtocol() {
    return protocol;
  }

  @Test
  public void sendEmail() throws Exception {
    runFlow(SEND_EMAIL);
    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Message sentMessage = messages[0];
    assertSubject(sentMessage.getSubject());
    assertBodyContent(process(sentMessage).getBody());
  }

  @Test
  public void sendEmailWithAttachment() throws Exception {
    runFlow(SEND_EMAIL_WITH_ATTACHMENT);
    Message[] messages = getReceivedMessagesAndAssertCount(4);
    for (Message message : messages) {
      Multipart content = (Multipart) message.getContent();
      assertThat(content.getCount(), is(3));

      Object body = content.getBodyPart(0).getContent();
      assertBodyContent((String) body);

      String textAttachment = (String) content.getBodyPart(1).getContent();
      assertThat(EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT, is(textAttachment));

      DataHandler jsonAttachment = content.getBodyPart(2).getDataHandler();
      assertThat(EMAIL_JSON_ATTACHMENT_CONTENT, is(IOUtils.toString((InputStream) jsonAttachment.getContent())));
    }
  }

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
    runFlow(REPLY_EMAIL);
  }

  @Test
  public void forwardEmail() throws Exception {
    flowRunner(FORWARD_EMAIL).withPayload(EMAIL_CONTENT).withAttributes(getTestAttributes()).run();

    Message[] messages = getReceivedMessagesAndAssertCount(1);
    String body = process(messages[0]).getBody();
    assertBodyContent(body);
  }

  @Test
  public void forwardEmailWithContent() throws Exception {
    flowRunner(FORWARD_EMAIL_WITH_CONTENT).withPayload(EMAIL_CONTENT).withAttributes(getTestAttributes()).run();

    Message[] messages = getReceivedMessagesAndAssertCount(1);
    String body = process(messages[0]).getBody();
    assertThat(body, containsString("More Content To Forward"));
    assertThat(body, containsString(EMAIL_CONTENT));
  }

  @Test
  public void sendEncodedMessage() throws Exception {
    final String defaultEncoding = muleContext.getConfiguration().getDefaultEncoding();
    Assert.assertThat(defaultEncoding, CoreMatchers.is(notNullValue()));

    final String customEncoding =
        availableCharsets().keySet().stream().filter(encoding -> !encoding.equals(defaultEncoding)).findFirst().orElse(null);

    assertThat(customEncoding, is(notNullValue()));

    flowRunner(SEND_ENCODED_MESSAGE).withPayload(WEIRD_CHAR_MESSAGE).withFlowVariable("encoding", customEncoding).run();

    Message[] messages = getReceivedMessagesAndAssertCount(1);
    Object content = ((String) messages[0].getContent()).trim();
    assertThat(content, is(new String(WEIRD_CHAR_MESSAGE.getBytes(customEncoding), customEncoding)));
  }

  private Message[] getReceivedMessagesAndAssertCount(int receivedNumber) {
    assertThat(server.waitForIncomingEmail(5000, receivedNumber), is(true));
    Message[] messages = server.getReceivedMessages();
    assertThat(messages, arrayWithSize(receivedNumber));
    return messages;
  }

  private EmailAttributes getTestAttributes() throws MessagingException {
    EmailAttributes attributes = mock(EmailAttributes.class);
    when(attributes.getCcAddresses()).thenReturn(singletonList(ALE_EMAIL));
    when(attributes.getToAddresses()).thenReturn(singletonList(JUANI_EMAIL));
    when(attributes.getSubject()).thenReturn(EMAIL_SUBJECT);
    return attributes;
  }
}
