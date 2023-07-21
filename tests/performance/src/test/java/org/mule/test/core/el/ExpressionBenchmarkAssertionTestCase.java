/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.el;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.el.ExpressionBenchmark;

import org.junit.Test;

public class ExpressionBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  private static final String EXPRESSION_PARAM = "expression";

  @Test
  public void evaluatePayloadMEL() {
    runAndAssertBenchmark(ExpressionBenchmark.class, "melPayload", 700, NANOSECONDS, 2000);
  }

  @Test
  public void evaluatePayloadDW() {
    // TODO MULE-11971 DW numbers are significantly below MEL currently but by adding assertions we avoid any regressions
    runAndAssertBenchmark(ExpressionBenchmark.class, "dwPayload", 5000, NANOSECONDS, 7000);
  }

}
