/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.EventBenchmark;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.session.DefaultMuleSession;
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
import org.mockito.runners.MockitoJUnitRunner;
import org.openjdk.jmh.annotations.Benchmark;

public class EventBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  public void createEvent() {
    runAndAssertBenchmark(EventBenchmark.class, "createEvent", 1, MICROSECONDS);
  }

  @Test
  public void copyEvent() {
    runAndAssertBenchmark(EventBenchmark.class, "copyEvent", 1, MICROSECONDS);
  }

  @Test
  public void copyEventWith20VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "copyEventWith20VariablesProperties", 1, MICROSECONDS);
  }

  @Test
  public void copyEventWith100VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "copyEventWith100VariablesProperties", 27, MICROSECONDS);
  }

  @Test
  public void deepCopyEvent() {
    runAndAssertBenchmark(EventBenchmark.class, "deepCopyEvent", 27, MICROSECONDS);
  }

  @Test
  public void deepCopyEventWith20VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "deepCopyEventWith20VariablesProperties", 27, MICROSECONDS);
  }

  @Test
  public void deepCopyEventWith50VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "deepCopyEventWith50VariablesProperties", 27, MICROSECONDS);
  }

  @Test
  public void deepCopyEventWith100VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "deepCopyEventWith100VariablesProperties", 27, MICROSECONDS);
  }

  @Test
  public void addEventVariable() {
    runAndAssertBenchmark(EventBenchmark.class, "addEventVariable", 27, MICROSECONDS);
  }

  @Test
  public void addEventVariableEventWith20VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "addEventVariableEventWith20VariablesProperties", 27, MICROSECONDS);
  }

  @Test
  public void addEventVariableEventWith50VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "addEventVariableEventWith50VariablesProperties", 27, MICROSECONDS);
  }

  @Test
  public void addEventVariableEventWith100VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "addEventVariableEventWith100VariablesProperties", 27, MICROSECONDS);
  }

  @Test
  public void copyWith10FlowVarsAnd10PropertiesWrite1OfEach() throws Exception {
    runAndAssertBenchmark(EventBenchmark.class, "copyWith10FlowVarsAnd10PropertiesWrite1OfEach", 3, MICROSECONDS);
  }

  @Test
  public void copyWith10FlowVarsAnd10PropertiesWrite5OfEach() throws Exception {
    runAndAssertBenchmark(EventBenchmark.class, "copyWith10FlowVarsAnd10PropertiesWrite1OfEach", 6, MICROSECONDS);
  }

  @Test
  public void copyWith50FlowVarsAnd50PropertiesWrite1OfEach() throws Exception {
    runAndAssertBenchmark(EventBenchmark.class, "copyWith10FlowVarsAnd10PropertiesWrite1OfEach", 8, MICROSECONDS);
  }

  @Test
  public void copyWith100FlowVarsAndPropertiesWrite25OfEach() throws Exception {
    runAndAssertBenchmark(EventBenchmark.class, "copyWith10FlowVarsAnd10PropertiesWrite1OfEach", 27, MICROSECONDS);
  }


}
