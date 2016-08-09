/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static javax.mail.Flags.Flag;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.RECENT;
import static javax.mail.Flags.Flag.SEEN;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameters;
import static org.mule.extension.email.internal.commands.EmailIdConsumerExecutor.NO_ID_ERROR;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.api.message.MuleMessage;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunnerDelegateTo(Parameterized.class)
public class IMAPTestCase extends AbstractEmailRetrieverTestCase {

  private static final String RETRIEVE_AND_DONT_READ = "retrieveAndDontRead";
  private static final String RETRIEVE_AND_MARK_AS_DELETE = "retrieveAndMarkDelete";
  private static final String RETRIEVE_AND_MARK_AS_READ = "retrieveAndMarkRead";
  private static final String RETRIEVE_MATCH_NOT_READ = "retrieveOnlyNotReadEmails";
  private static final String RETRIEVE_AND_DELETE_INCOMING_AND_SCHEDULED = "retrieveAndDeleteIncomingAndScheduled";
  private static final String RETRIEVE_MATCH_RECENT = "retrieveOnlyRecentEmails";
  private static final String FAIL_SETTING_FLAG = "failSettingFlag";

  @Parameter
  public String protocol;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"imap"}, {"imaps"}});
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
    List<MuleMessage> messages = runFlowAndGetMessages(RETRIEVE_AND_READ);
    assertThat(messages, hasSize(10));
    messages.forEach(m -> {
      assertBodyContent((String) m.getPayload());
      assertThat(((EmailAttributes) m.getAttributes()).getFlags().isSeen(), is(true));
    });
  }

  @Test
  public void retrieveAndDontRead() throws Exception {
    List<MuleMessage> messages = runFlowAndGetMessages(RETRIEVE_AND_DONT_READ);
    assertThat(messages, hasSize(10));
    messages.forEach(m -> assertThat(((EmailAttributes) m.getAttributes()).getFlags().isSeen(), is(false)));
  }

  @Test
  public void retrieveAndThenRead() throws Exception {
    runFlow(RETRIEVE_AND_MARK_AS_READ);
    MimeMessage[] messages = server.getReceivedMessages();
    assertThat(messages, arrayWithSize(10));
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, SEEN, true));
  }

  @Test
  public void retrieveAndMarkAsDelete() throws Exception {
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, DELETED, false));
    runFlow(RETRIEVE_AND_MARK_AS_DELETE);
    assertThat(server.getReceivedMessages(), arrayWithSize(10));
    stream(server.getReceivedMessages()).forEach(m -> assertFlag(m, DELETED, true));
  }

  @Test
  public void failSettingFlag() throws Exception {
    expectedException.expectCause(instanceOf(EmailException.class));
    expectedException.expectMessage(NO_ID_ERROR);
    runFlow(FAIL_SETTING_FLAG);
  }

  @Test
  public void retrieveAndDeleteIncomingAndScheduled() throws Exception {
    MimeMessage[] startMessageBatch = server.getReceivedMessages();
    assertThat(startMessageBatch, arrayWithSize(10));
    startMessageBatch[0].setFlag(DELETED, true); // Scheduled for deletion
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

  private void testMatcherFlag(String flowName, Flag flag, boolean flagState) throws Exception {
    for (int i = 0; i < 3; i++) {
      MimeMessage message = server.getReceivedMessages()[i];
      message.setFlag(flag, flagState);
    }

    List<MuleMessage> messages = runFlowAndGetMessages(flowName);
    assertThat(server.getReceivedMessages(), arrayWithSize(10));
    assertThat(messages, hasSize(7));
  }
}
