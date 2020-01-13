/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;

import java.util.Optional;

public class ExecutionUtils {

  public static Optional<MutableConfigurationStats> getMutableConfigurationStats(ExecutionContext<?> context) {
    return context.getConfiguration()
        .map(ConfigurationInstance::getStatistics)
        .filter(s -> s instanceof MutableConfigurationStats)
        .map(s -> (MutableConfigurationStats) s);
  }
}
