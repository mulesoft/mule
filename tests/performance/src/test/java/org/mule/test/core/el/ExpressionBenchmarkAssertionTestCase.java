/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.el;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.el.SimpleExpressionBenchmark;

import org.junit.Test;

public class ExpressionBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  private static final String EXPRESSION_PARAM = "expression";

  @Test
  public void evaluatePayloadMEL() {
    runAndAssertBenchmark(SimpleExpressionBenchmark.class, "evaluatePayload", 1, singletonMap(EXPRESSION_PARAM,
                                                                                              new String[] {
                                                                                                  "mel:payload"}),
                          700, NANOSECONDS, 2000);
  }

  @Test
  public void evaluatePayloadDW() {
    // TODO MULE-11971 DW numbers are significantly below MEL currently but by adding assertions we avoid any regressions
    runAndAssertBenchmark(SimpleExpressionBenchmark.class, "evaluatePayload", 1, singletonMap(EXPRESSION_PARAM,
                                                                                              new String[] {
                                                                                                  "payload"}),
                          5000, NANOSECONDS, 7000);
  }

}
