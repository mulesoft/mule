/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

/**
 *
 * @since 4.10
 */
public class PilotFlowsSummaryStatistics extends DefaultFlowsSummaryStatistics {

  public PilotFlowsSummaryStatistics(boolean isStatisticsEnabled) {
    super(isStatisticsEnabled);
  }

  protected String getMetricSuffix() {
    return "-pilot";
  }
}
