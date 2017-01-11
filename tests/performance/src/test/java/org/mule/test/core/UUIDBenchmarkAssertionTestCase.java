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
import org.mule.UUIDBenchmark;

import org.junit.Test;

public class UUIDBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  public void singleThread() {
    runAndAssertBenchmark(UUIDBenchmark.class, "UUID", 250, NANOSECONDS, 250);
  }

  @Test
  public void tenThreads() throws Exception {
    runAndAssertBenchmark(UUIDBenchmark.class, "UUID", 10, 2, MICROSECONDS);
  }

  @Test
  public void twentyThreads() throws Exception {
    runAndAssertBenchmark(UUIDBenchmark.class, "UUID", 20, 3, MICROSECONDS);
  }

  @Test
  public void fiftyThreads() throws Exception {
    runAndAssertBenchmark(UUIDBenchmark.class, "UUID", 50, 8, MICROSECONDS);
  }

  @Test
  public void hundredThreads() throws Exception {
    runAndAssertBenchmark(UUIDBenchmark.class, "UUID", 100, 15, MICROSECONDS);
  }

  @Test
  public void twoHundredThreads() throws Exception {
    runAndAssertBenchmark(UUIDBenchmark.class, "UUID", 200, 40, MICROSECONDS);
  }

}
