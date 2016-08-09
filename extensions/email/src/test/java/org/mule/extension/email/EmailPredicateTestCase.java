/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.EmailPredicateBuilder;
import org.mule.extension.email.api.EmailFlags;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;

public class EmailPredicateTestCase {

  private static final LocalDateTime RECEIVED_DATE = LocalDateTime.of(2015, 4, 20, 00, 00);
  private static final LocalDateTime SENT_DATE = LocalDateTime.of(2014, 4, 10, 00, 00);

  private EmailAttributes attributes;
  private EmailPredicateBuilder builder;

  @Before
  public void before() {
    builder = new EmailPredicateBuilder();

    EmailFlags flags = mock(EmailFlags.class);
    when(flags.isSeen()).thenReturn(true);
    when(flags.isRecent()).thenReturn(true);
    when(flags.isDeleted()).thenReturn(false);

    attributes = mock(EmailAttributes.class);
    when(attributes.getSubject()).thenReturn(EMAIL_SUBJECT);
    when(attributes.getFromAddresses()).thenReturn(singletonList(JUANI_EMAIL));
    when(attributes.getFlags()).thenReturn(flags);
    when(attributes.getReceivedDate()).thenReturn(RECEIVED_DATE);
    when(attributes.getSentDate()).thenReturn(SENT_DATE);
  }


  @Test
  public void matchSubjectRegex() {
    builder.setSubjectRegex("Email.*");
    assertMatch();
  }

  @Test
  public void rejectSubjectRegex() {
    builder.setSubjectRegex("RejectSubject");
    assertReject();
  }

  @Test
  public void matchFromRegex() {
    builder.setFromRegex(".*@mulesoft.com");
    assertMatch();
  }

  @Test
  public void rejectFromRegex() {
    builder.setFromRegex(".*@google.com");
    assertReject();
  }

  @Test
  public void matchSeen() {
    builder.setSeen(true);
    assertMatch();
  }

  @Test
  public void rejectSeen() {
    builder.setSeen(false);
    assertReject();
  }

  @Test
  public void matchRecent() {
    builder.setRecent(true);
    assertMatch();
  }

  @Test
  public void rejectRecent() {
    builder.setRecent(false);
    assertReject();
  }

  @Test
  public void matchReceivedDate() {
    builder.setReceivedSince(RECEIVED_DATE.minusYears(1));
    builder.setReceivedUntil(RECEIVED_DATE.plusYears(1));
    assertMatch();
  }

  @Test
  public void matchSentDate() {
    builder.setSentSince(SENT_DATE.minusYears(1));
    builder.setSentUntil(SENT_DATE.plusYears(1));
    assertMatch();
  }

  @Test
  public void rejectReceivedDate() {
    builder.setReceivedSince(RECEIVED_DATE.plusYears(1));
    assertReject();
  }

  private void assertMatch() {
    assertThat(builder.build().test(attributes), is(true));
  }

  private void assertReject() {
    assertThat(builder.build().test(attributes), is(false));
  }

}
