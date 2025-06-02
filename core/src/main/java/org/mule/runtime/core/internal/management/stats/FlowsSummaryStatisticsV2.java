/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

/**
 * This variation of {@link DefaultFlowsSummaryStatistics} only applies a different suffix to the metrics when exported. The
 * difference in the computation of the values lies elsewhere as these classes are mostly just containers.
 *
 * @since 4.10
 */
public class FlowsSummaryStatisticsV2 extends DefaultFlowsSummaryStatistics {

  public FlowsSummaryStatisticsV2(boolean isStatisticsEnabled) {
    super(isStatisticsEnabled);
  }

  protected String getMetricSuffix() {
    return "-v2";
  }
}
