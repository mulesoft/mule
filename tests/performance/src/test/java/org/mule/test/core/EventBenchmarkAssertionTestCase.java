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
import org.mule.EventBenchmark;

import org.junit.Test;

public class EventBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  public void createEvent() {
    runAndAssertBenchmark(EventBenchmark.class, "createEvent", 2, MICROSECONDS, 3000);
  }

  @Test
  public void copyEvent() {
    runAndAssertBenchmark(EventBenchmark.class, "copyEvent", 250, NANOSECONDS, 600);
  }

  @Test
  public void copyEventWith20VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "copyEventWith20VariablesProperties", 2, MICROSECONDS, 2300);
  }

  @Test
  public void copyEventWith100VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "copyEventWith100VariablesProperties", 15, MICROSECONDS, 11000);
  }

  @Test
  public void deepCopyEvent() {
    runAndAssertBenchmark(EventBenchmark.class, "deepCopyEvent", 1, MICROSECONDS, 2000);
  }

  @Test
  public void deepCopyEventWith20VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "deepCopyEventWith20VariablesProperties", 12, MICROSECONDS, 12200);
  }

  @Test
  public void deepCopyEventWith50VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "deepCopyEventWith50VariablesProperties", 65, MICROSECONDS, 56300);
  }

  @Test
  public void deepCopyEventWith100VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "deepCopyEventWith100VariablesProperties", 75, MICROSECONDS, 56300);
  }

  @Test
  public void addEventVariable() {
    runAndAssertBenchmark(EventBenchmark.class, "addEventVariable", 800, NANOSECONDS, 1300);
  }

  @Test
  public void addEventVariableEventWith20VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "addEventVariableEventWith20VariablesProperties", 10, MICROSECONDS, 4700);
  }

  @Test
  public void addEventVariableEventWith50VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "addEventVariableEventWith50VariablesProperties", 25, MICROSECONDS, 21200);
  }

  @Test
  public void addEventVariableEventWith100VariablesProperties() {
    runAndAssertBenchmark(EventBenchmark.class, "addEventVariableEventWith100VariablesProperties", 25, MICROSECONDS, 21200);
  }

  @Test
  public void copyWith10FlowVarsAnd10PropertiesWrite1OfEach() throws Exception {
    runAndAssertBenchmark(EventBenchmark.class, "copyWith10FlowVarsAnd10PropertiesWrite1OfEach", 15, MICROSECONDS, 13700);
  }

  @Test
  public void copyWith10FlowVarsAnd10PropertiesWrite5OfEach() throws Exception {
    runAndAssertBenchmark(EventBenchmark.class, "copyWith10FlowVarsAnd10PropertiesWrite5OfEach", 15, MICROSECONDS, 85900);
  }

  @Test
  public void copyWith50FlowVarsAnd50PropertiesWrite1OfEach() throws Exception {
    runAndAssertBenchmark(EventBenchmark.class, "copyWith50FlowVarsAnd50PropertiesWrite1OfEach", 20, MICROSECONDS, 57700);
  }

  @Test
  public void copyWith100FlowVarsAndPropertiesWrite25OfEach() throws Exception {
    runAndAssertBenchmark(EventBenchmark.class, "copyWith100FlowVarsAndPropertiesWrite25OfEach", 15, MICROSECONDS, 160300);
  }


}
