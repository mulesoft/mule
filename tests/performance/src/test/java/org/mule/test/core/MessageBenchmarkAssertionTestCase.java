/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.MessageBenchmark;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openjdk.jmh.annotations.Benchmark;

public class MessageBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  public void createMessage() {
    runAndAssertBenchmark(MessageBenchmark.class, "createMessage", 27, MICROSECONDS);
  }

  @Test
  public void createMessageWithDataType() {
    runAndAssertBenchmark(MessageBenchmark.class, "createMessageWithDataType", 27, MICROSECONDS);
  }

  @Test
  public void copyMessage() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyMessage", 27, MICROSECONDS);
  }

  @Test
  public void copyMessageWith20Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyMessageWith20Properties", 27, MICROSECONDS);
  }

  @Test
  public void copyMessageWith100Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyMessageWith100Properties", 27, MICROSECONDS);
  }

  @Test
  public void mutateMessagePayload() {
    runAndAssertBenchmark(MessageBenchmark.class, "mutateMessagePayload", 27, MICROSECONDS);
  }

  @Test
  public void mutateMessagePayloadWithDataType() {
    runAndAssertBenchmark(MessageBenchmark.class, "mutateMessagePayloadWithDataType", 27, MICROSECONDS);
  }

  @Test
  public void addMessageProperty() {
    runAndAssertBenchmark(MessageBenchmark.class, "addMessageProperty", 27, MICROSECONDS);
  }

  @Test
  public void addMessagePropertyMessageWith20Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "addMessagePropertyMessageWith20Properties", 27, MICROSECONDS);
  }

  @Test
  public void addMessagePropertyMessageWith100Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "addMessagePropertyMessageWith100Properties", 27, MICROSECONDS);
  }

  @Test
  public void addMessagePropertyWithDataType() {
    runAndAssertBenchmark(MessageBenchmark.class, "addMessagePropertyWithDataType", 27, MICROSECONDS);
  }

  @Test
  public void addRemoveMessageProperty() {
    runAndAssertBenchmark(MessageBenchmark.class, "addRemoveMessageProperty", 27, MICROSECONDS);
  }

  @Test
  public void addRemoveMessagePropertyMessageWith20Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "addRemoveMessagePropertyMessageWith20Properties", 27, MICROSECONDS);
  }

  @Test
  public void addRemoveMessagePropertyMessageWith100Properties() {
    runAndAssertBenchmark(MessageBenchmark.class, "addRemoveMessagePropertyMessageWith100Properties", 27, MICROSECONDS);
  }

  @Test
  public void addRemoveMessagePropertyWithDataType() {
    runAndAssertBenchmark(MessageBenchmark.class, "addRemoveMessagePropertyWithDataType", 27, MICROSECONDS);
  }

  @Test
  public void copyWith20PropertiesWrite1Outbound() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyWith20PropertiesWrite1Outbound", 50, MICROSECONDS);
  }

  @Test
  public void copyWith20PopertiesWrite100Outbound() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyWith20PopertiesWrite100Outbound", 65, MICROSECONDS);
  }

  @Test
  public void copyWith100PropertiesWrite1Outbound() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyWith100PropertiesWrite1Outbound", 65, MICROSECONDS);
  }

  @Test
  public void copyWith100PropertiesWrite50Outbound() {
    runAndAssertBenchmark(MessageBenchmark.class, "copyWith100PropertiesWrite50Outbound", 65, MICROSECONDS);
  }

}
