/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.routing;

import org.mule.runtime.core.api.management.stats.RouterStatistics;

public interface RouterStatisticsRecorder {

  void setRouterStatistics(RouterStatistics stats);

}
