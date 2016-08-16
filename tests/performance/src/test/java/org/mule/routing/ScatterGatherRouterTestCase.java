/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.functional.junit4.FunctionalTestCase;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.Required;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;

public class ScatterGatherRouterTestCase extends FunctionalTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  @Override
  public int getTestTimeoutSecs() {
    return 120;
  }

  @Override
  protected String getConfigFile() {
    return "scatter-gather-perf-test.xml";
  }

  @Test
  @Required(throughput = 180, average = 6, percentile90 = 7)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void parallelProcessing() throws Exception {
    this.runFlow("parallelProcessing");
  }

  @Test
  @Required(throughput = 50, average = 18, percentile90 = 20)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void sequentialProcessing() throws Exception {
    this.runFlow("sequentialProcessing");
  }

  @Test
  @Required(throughput = 220, average = 5, percentile90 = 6)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void parallelHttpProcessing() throws Exception {
    this.runFlow("parallelHttpProcessing");
  }

  @Test
  @Required(throughput = 200, average = 5, percentile90 = 6)
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void sequentialHttpProcessing() throws Exception {
    this.runFlow("sequentialHttpProcessing");
  }

  @Test
  @Required(throughput = 1600, average = 6, percentile90 = 13)
  @PerfTest(duration = 15000, threads = 10, warmUp = 5000)
  public void parallelHttMultiThreadedProcessing() throws Exception {
    this.runFlow("parallelHttpProcessing");
  }

  @Test
  @Required(throughput = 2000, average = 5, percentile90 = 6)
  @PerfTest(duration = 15000, threads = 10, warmUp = 5000)
  public void sequentialHttpMultiThreadedProcessing() throws Exception {
    this.runFlow("sequentialHttpProcessing");
  }
}
