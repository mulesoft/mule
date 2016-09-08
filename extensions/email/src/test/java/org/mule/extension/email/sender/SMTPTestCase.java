/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public abstract class SMTPTestCase extends EmailConnectorTestCase {

  static final String WEIRD_CHAR_MESSAGE = "This is a messag\u00ea with weird chars \u00f1.";

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

  Message[] getReceivedMessagesAndAssertCount(int receivedNumber) {
    assertThat(server.waitForIncomingEmail(5000, receivedNumber), is(true));
    Message[] messages = server.getReceivedMessages();
    assertThat(messages, arrayWithSize(receivedNumber));
    return messages;
  }

  EmailAttributes getTestAttributes() throws MessagingException {
    EmailAttributes attributes = mock(EmailAttributes.class);
    when(attributes.getCcAddresses()).thenReturn(singletonList(ALE_EMAIL));
    when(attributes.getToAddresses()).thenReturn(singletonList(JUANI_EMAIL));
    when(attributes.getSubject()).thenReturn(EMAIL_SUBJECT);
    return attributes;
  }

  void assertJsonAttachment(DataHandler jsonAttachment) throws IOException {
    assertThat(EMAIL_JSON_ATTACHMENT_CONTENT, is(IOUtils.toString((InputStream) jsonAttachment.getContent())));
  }
}
