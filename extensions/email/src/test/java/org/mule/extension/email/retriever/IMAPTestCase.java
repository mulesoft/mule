/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.RECENT;
import static javax.mail.Flags.Flag.SEEN;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.api.exception.EmailErrors.EMAIL_NOT_FOUND;
import static org.mule.extension.email.util.EmailTestUtils.ALE_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.ESTEBAN_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.extension.email.api.exception.EmailNotFoundException;
import org.mule.runtime.core.streaming.ConsumerIterator;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class IMAPTestCase extends AbstractEmailRetrieverTestCase {

  private static final String RETRIEVE_AND_DONT_READ = "retrieveAndDontRead";
  private static final String RETRIEVE_AND_MARK_AS_DELETE = "retrieveAndMarkDelete";
  private static final String RETRIEVE_AND_MARK_AS_READ = "retrieveAndMarkRead";
  private static final String RETRIEVE_MATCH_NOT_READ = "retrieveOnlyNotReadEmails";
  private static final String RETRIEVE_AND_DELETE_INCOMING_AND_SCHEDULED = "retrieveAndDeleteIncomingAndScheduled";
  private static final String RETRIEVE_MATCH_RECENT = "retrieveOnlyRecentEmails";
  private static final String FAIL_MARKING_FLAG = "failMarkingEmail";
  private static final String RETRIEVE_DELETE_SELECTED = "retrieveAndDeleteSelected";

  @Parameterized.Parameter
  public String protocol;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{"imap"}, {"imaps"}});
  }

  @Override
  protected String getConfigFile() {
    return format("retriever/%s.xml", protocol);
  }

  @Override
  public String getProtocol() {
    return protocol;
  }

  @Test
  public void retrieveAndRead() throws Exception {
    ConsumerIterator<Result> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    int size = 0;
    while (messages.hasNext()) {
      size++;
      Result m = messages.next();
      assertBodyContent((String) m.getOutput());
      assertThat(((IMAPEmailAttributes) m.getAttributes().get()).getFlags().isSeen(), is(true));
    }

    assertThat(size, is(pageSize));
  }

  @Test
  public void retrieveAndDontRead() throws Exception {
    ConsumerIterator<Result> messages = runFlowAndGetMessages(RETRIEVE_AND_DONT_READ);
    int count = 0;
    while (messages.hasNext()) {
      Result m = messages.next();
      assertThat(((IMAPEmailAttributes) m.getAttributes().get()).getFlags().isSeen(), is(false));
      count++;
    }
    assertThat(count, is(pageSize));
  }

  @Test
  public void retrieveAndThenRead() throws Exception {
    flowRunner(RETRIEVE_AND_MARK_AS_READ).run();
    MimeMessage[] messages = server.getReceivedMessages();
    assertThat(messages, arrayWithSize(10));
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, SEEN, true));
  }

  @Test
  public void retrieveAndMarkAsDelete() throws Exception {
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, DELETED, false));
    runFlow(RETRIEVE_AND_MARK_AS_DELETE);
    assertThat(server.getReceivedMessages(), arrayWithSize(pageSize));
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, DELETED, true));
  }

  @Test
  public void failSettingFlag() throws Exception {
    expectedError.expectError(NAMESPACE, EMAIL_NOT_FOUND.getType(), EmailNotFoundException.class,
                              "No email was found with id:[0]");
    runFlow(FAIL_MARKING_FLAG);
  }

  @Test
  public void retrieveAndDeleteIncomingAndScheduled() throws Exception {
    MimeMessage[] startMessageBatch = server.getReceivedMessages();
    assertThat(startMessageBatch, arrayWithSize(pageSize));
    // Scheduled for deletion
    startMessageBatch[0].setFlag(DELETED, true);
    runFlow(RETRIEVE_AND_DELETE_INCOMING_AND_SCHEDULED);
    assertThat(server.getReceivedMessages(), arrayWithSize(8));
  }

  @Test
  public void retrieveOnlyNotRead() throws Exception {
    testMatcherFlag(RETRIEVE_MATCH_NOT_READ, SEEN, true);
  }

  @Test
  public void retrieveOnlyRecent() throws Exception {
    testMatcherFlag(RETRIEVE_MATCH_RECENT, RECENT, false);
  }

  @Test
  public void retrieveAndExpungeDelete() throws Exception {
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, DELETED, false));
    runFlow(RETRIEVE_AND_THEN_EXPUNGE_DELETE);
    assertThat(server.getReceivedMessages().length, is(0));
  }

  @Test
  public void storeEmailsInDirectory() throws Exception {
    flowRunner(STORE_MESSAGES).run();
    File[] storedEmails = temporaryFolder.getRoot().listFiles();
    assertThat(storedEmails, is(not(nullValue())));
    assertThat(storedEmails, arrayWithSize(pageSize));
    for (File storedEmail : storedEmails) {
      assertStoredEmail(storedEmail);
    }
  }

  @Test
  public void storeSingleEmailInDirectory() throws Exception {
    flowRunner(STORE_SINGLE_MESSAGE).run();
    File[] storedEmails = temporaryFolder.getRoot().listFiles();
    assertThat(storedEmails, is(not(nullValue())));
    assertThat(storedEmails, arrayWithSize(1));
    assertStoredEmail(storedEmails[0]);
  }

  @Test
  public void retrieveAndDeleteSelectedEmails() throws Exception {
    assertThat(server.getReceivedMessages(), arrayWithSize(pageSize));
    runFlow(RETRIEVE_DELETE_SELECTED);
    assertThat(server.getReceivedMessages(), arrayWithSize(5));
  }

  private void testMatcherFlag(String flowName, Flag flag, boolean flagState) throws Exception {
    for (int i = 0; i < 3; i++) {
      MimeMessage message = server.getReceivedMessages()[i];
      message.setFlag(flag, flagState);
    }

    ConsumerIterator<Result> messages = runFlowAndGetMessages(flowName);
    assertThat(server.getReceivedMessages(), arrayWithSize(pageSize));
    int count = 0;
    while (messages.hasNext()) {
      messages.next();
      count++;
    }
    assertThat(count, is(7));
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
}
