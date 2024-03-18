/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
