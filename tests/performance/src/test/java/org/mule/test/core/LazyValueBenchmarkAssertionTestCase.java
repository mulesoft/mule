/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
