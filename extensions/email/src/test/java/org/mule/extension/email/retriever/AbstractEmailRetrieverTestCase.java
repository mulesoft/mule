/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.extension.email.api.MessageBuilder.newMessage;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.ESTEBAN_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.assertAttachmentContent;
import static org.mule.extension.email.util.EmailTestUtils.getMultipartTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.testSession;

import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.mail.Flags.Flag;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractEmailRetrieverTestCase extends EmailConnectorTestCase {

  protected static final String RETRIEVE_AND_READ = "retrieveAndRead";
  protected static final String RETRIEVE_AND_DELETE = "retrieveAndDelete";
  protected static final String RETRIEVE_AND_THEN_EXPUNGE_DELETE = "retrieveAndThenExpungeDelete";
  protected static final String RETRIEVE_MATCH_SUBJECT_AND_FROM = "retrieveMatchingSubjectAndFromAddress";
  protected static final String RETRIEVE_WITH_ATTACHMENTS = "retrieveWithAttachments";
  protected static final String STORE_MESSAGES = "storeMessages";
  protected static final String STORE_SINGLE_MESSAGE = "storeSingleMessage";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty temporaryFolderProperty = new SystemProperty("storePath", temporaryFolder.getRoot().getAbsolutePath());

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    temporaryFolder.delete();
  }

  @Before
  public void sendInitialEmailBatch() throws MessagingException {
    for (int i = 0; i < 10; i++) {
      user.deliver(newMessage(testSession).to(singletonList(JUANI_EMAIL)).fromAddresses(ESTEBAN_EMAIL)
          .cc(singletonList(ALE_EMAIL)).withContent(EMAIL_CONTENT).withSubject(EMAIL_SUBJECT).build());
    }
  }

  @Test
  public void retrieveNothing() throws Exception {
    server.purgeEmailFromAllMailboxes();
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
    List<MuleMessage> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    assertThat(messages, hasSize(0));
  }

  @Test
  public void retrieveMatchingSubjectAndFromAddress() throws Exception {
    for (int i = 0; i < 5; i++) {
      String fromEmail = format("address.%s@enterprise.com", i);
      user.deliver(newMessage(testSession).to(singletonList(ESTEBAN_EMAIL)).cc(singletonList(ALE_EMAIL))
          .withContent(EMAIL_CONTENT).withSubject("Non Matching Subject").fromAddresses(fromEmail).build());
    }

    List<MuleMessage> messages = runFlowAndGetMessages(RETRIEVE_MATCH_SUBJECT_AND_FROM);
    assertThat(server.getReceivedMessages(), arrayWithSize(15));
    assertThat(messages, hasSize(10));
  }

  @Test
  public void retrieveEmailWithAttachments() throws Exception {
    server.purgeEmailFromAllMailboxes();
    user.deliver(getMultipartTestMessage());
    List<MuleMessage> messages = runFlowAndGetMessages(RETRIEVE_WITH_ATTACHMENTS);

    assertThat(messages, hasSize(1));
    assertThat(messages.get(0).getPayload(), instanceOf(MultiPartPayload.class));
    List<MuleMessage> emailAttachments = ((MultiPartPayload) messages.get(0).getPayload()).getParts();

    assertThat(emailAttachments, hasSize(3));
    assertThat(((DefaultMultiPartPayload) messages.get(0).getPayload()).hasBodyPart(), is(true));
    assertThat(((MultiPartPayload) messages.get(0).getPayload()).getPartNames(),
               hasItems(EMAIL_JSON_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME));
    assertAttachmentContent(emailAttachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
    assertAttachmentContent(emailAttachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
  }

  @Test
  public void retrieveAndDelete() throws Exception {
    assertThat(server.getReceivedMessages(), arrayWithSize(10));
    runFlow(RETRIEVE_AND_DELETE);
    assertThat(server.getReceivedMessages(), arrayWithSize(0));
  }

  @Test
  public void storeEmailsInDirectory() throws Exception {
    runFlow(STORE_MESSAGES);
    File[] storedEmails = temporaryFolder.getRoot().listFiles();
    assertThat(storedEmails, is(not(nullValue())));
    assertThat(storedEmails, arrayWithSize(10));
    for (File storedEmail : storedEmails) {
      assertStoredEmail(storedEmail);
    }
  }

  @Test
  public void storeSingleEmailInDirectory() throws Exception {
    runFlow(STORE_SINGLE_MESSAGE);
    File[] storedEmails = temporaryFolder.getRoot().listFiles();
    assertThat(storedEmails, is(not(nullValue())));
    assertThat(storedEmails, arrayWithSize(1));
    assertStoredEmail(storedEmails[0]);
  }

  private void assertStoredEmail(File storedEmail) throws IOException {
    assertThat(storedEmail.getName(), startsWith(EMAIL_SUBJECT));
    String fileContent = new String(Files.readAllBytes(storedEmail.toPath()));
    assertThat(fileContent, containsString("To: " + JUANI_EMAIL));
    assertThat(fileContent, containsString("From: " + ESTEBAN_EMAIL));
    assertThat(fileContent, containsString("Cc: " + ALE_EMAIL));
    assertThat(fileContent, containsString("Subject: " + EMAIL_SUBJECT));
    assertThat(fileContent, containsString(EMAIL_CONTENT));
  }

  protected List<MuleMessage> runFlowAndGetMessages(String flowName) throws Exception {
    return (List<MuleMessage>) runFlow(flowName).getMessage().getPayload();
  }

  protected void assertFlag(MimeMessage m, Flag flag, boolean contains) {
    try {
      assertThat(m.getFlags().contains(flag), is(contains));
    } catch (MessagingException e) {
      fail("flag assertion error");
    }
  }
}
