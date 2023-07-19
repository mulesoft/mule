/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.connector;

/**
 * This determines whether polling is turned on or off
 */
public interface SchedulerController {

  /**
   * @return true if this runtime instance is the one running jobs that must be executed in only one runtime instance, false
   *         otherwise.
   */
  boolean isPrimarySchedulingInstance();

}
