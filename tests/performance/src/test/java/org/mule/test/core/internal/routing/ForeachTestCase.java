/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.internal.routing;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.junit.Ignore;
import org.junit.Test;
import org.mule.AbstractBenchmarkAssertionTestCase;
import org.mule.runtime.core.internal.routing.ForeachBenchmark;

public class ForeachTestCase extends AbstractBenchmarkAssertionTestCase {

  @Test
  @Ignore
  public void singleForeach() {
    runAndAssertBenchmark(ForeachBenchmark.class, "singleForeach", 500, MILLISECONDS);
  }

  @Test

  public void nestedForeach() {
    runAndAssertBenchmark(ForeachBenchmark.class, "nestedForeach", 4200, MILLISECONDS);
  }

  @Test
  @Ignore
  public void multiplesThreadsUsingSameForeach() {
    runAndAssertBenchmark(ForeachBenchmark.class, "multiplesThreadsUsingSameForeach", 1400, MILLISECONDS);
  }
}
