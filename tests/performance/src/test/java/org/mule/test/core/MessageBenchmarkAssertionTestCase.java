/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.MessageBenchmark;

import org.junit.Test;

public class MessageBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  public void createMessage() {
    runAndAssertBenchmark(MessageBenchmark.class, "createMessage", 500, NANOSECONDS, 800);
  }

  @Test
  public void createMessageWithDataType() {
    runAndAssertBenchmark(MessageBenchmark.class, "createMessageWithDataType", 500, NANOSECONDS, 900);
  }

  @Test
  public void copyMessage() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyMessage", 700, NANOSECONDS, 900);
  }

  @Test
  public void copyMessageWith20Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyMessageWith20Properties", 50, MICROSECONDS, 28700);
  }

  @Test
  public void copyMessageWith100Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyMessageWith100Properties", 200, MICROSECONDS, 144300);
  }

  @Test
  public void mutateMessagePayload() {
    runAndAssertBenchmark(MessageBenchmark.class, "mutateMessagePayload", 500, NANOSECONDS, 900);
  }

  @Test
  public void mutateMessagePayloadWithDataType() {
    runAndAssertBenchmark(MessageBenchmark.class, "mutateMessagePayloadWithDataType", 500, NANOSECONDS, 1000);
  }

  @Test
  public void addMessageProperty() {
    runAndAssertBenchmark(MessageBenchmark.class, "addMessageProperty", 1, MICROSECONDS, 1300);
  }

  @Test
  public void addMessagePropertyMessageWith20Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "addMessagePropertyMessageWith20Properties", 30, MICROSECONDS, 1000);
  }

  @Test
  public void addMessagePropertyMessageWith100Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "addMessagePropertyMessageWith100Properties", 200, MICROSECONDS, 44600);
  }

  @Test
  public void addMessagePropertyWithDataType() {
    runAndAssertBenchmark(MessageBenchmark.class, "addMessagePropertyWithDataType", 600, NANOSECONDS, 1200);
  }

  @Test
  public void addRemoveMessageProperty() {
    runAndAssertBenchmark(MessageBenchmark.class, "addRemoveMessageProperty", 2, MICROSECONDS, 2600);
  }

  @Test
  public void addRemoveMessagePropertyMessageWith20Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "addRemoveMessagePropertyMessageWith20Properties", 65, MICROSECONDS, 57800);
  }

  @Test
  public void addRemoveMessagePropertyMessageWith100Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "addRemoveMessagePropertyMessageWith100Properties", 350, MICROSECONDS, 289000);
  }

  @Test
  public void addRemoveMessagePropertyWithDataType() {
    runAndAssertBenchmark(MessageBenchmark.class, "addRemoveMessagePropertyWithDataType", 1200, NANOSECONDS, 2400);
  }

  @Test
  public void copyWith20PropertiesWrite1Outbound() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyWith20PropertiesWrite1Outbound", 50, MICROSECONDS, 33300);
  }

  @Test
  public void copyWith20PopertiesWrite100Outbound() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyWith20PopertiesWrite100Outbound", 65, MICROSECONDS, 7300);
  }

  @Test
  public void copyWith100PropertiesWrite1Outbound() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyWith100PropertiesWrite1Outbound", 250, MICROSECONDS, 148500);
  }

  @Test
  public void copyWith100PropertiesWrite50Outbound() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyWith100PropertiesWrite50Outbound", 200, MICROSECONDS, 165200);
  }

}
