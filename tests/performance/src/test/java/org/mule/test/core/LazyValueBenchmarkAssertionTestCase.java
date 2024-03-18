/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.LazyValueBenchmark;

import org.junit.Test;

public class LazyValueBenchmarkAssertionTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  public void lazyAccess() {
    runAndAssertBenchmark(LazyValueBenchmark.class, "lazyAccess", 500, NANOSECONDS, 800);
  }

  @Test
  public void notSoLazyAccess() {
    runAndAssertBenchmark(LazyValueBenchmark.class, "notSoLazyAccess", 500, NANOSECONDS, 800);
  }

  @Test
  public void lazyIfComputed() {
    runAndAssertBenchmark(LazyValueBenchmark.class, "lazyIfComputed", 500, NANOSECONDS, 800);
  }

  @Test
  public void notSoLazyIfComputed() {
    runAndAssertBenchmark(LazyValueBenchmark.class, "notSoLazyIfComputed", 500, NANOSECONDS, 800);
  }
}
