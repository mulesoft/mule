/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.manager;

/**
 * Monitors configuration instances looking for those which should be expired.
 * <p/>
 * It only exposes {@link #beginMonitoring()} and {@link #stopMonitoring()} method to signal when monitoring should start/stop.
 *
 * @since 4.0
 */
public interface ConfigurationExpirationMonitor {

  /**
   * Begins monitoring and disposal of expired configuration instances
   */
  void beginMonitoring();

  /**
   * Stops monitoring and disposal of expired configuration instances
   */
  void stopMonitoring();
}
