/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.internal.routing;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.junit.Test;
import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.runtime.core.internal.routing.ForeachBenchmark;

public class ForeachTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  public void singleForeach() {
    runAndAssertBenchmark(ForeachBenchmark.class, "singleForeach", 1000, MILLISECONDS);
  }

  @Test
  public void nestedForeach() {
    runAndAssertBenchmark(ForeachBenchmark.class, "nestedForeach", 6000, MILLISECONDS);
  }

  @Test
  public void multiplesThreadsUsingSameForeach() {
    runAndAssertBenchmark(ForeachBenchmark.class, "multiplesThreadsUsingSameForeach", 3000, MILLISECONDS);
  }
}
