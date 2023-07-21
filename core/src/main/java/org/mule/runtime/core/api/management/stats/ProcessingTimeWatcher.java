/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;

/**
 * Watches {@link ProcessingTime} instances to detect when they are weakly reachable.
 */
@NoImplement
public interface ProcessingTimeWatcher extends Startable, Stoppable {

  /**
   * Adds a new instance to watch
   *
   * @param processingTime instance to add. Non null
   */
  void addProcessingTime(ProcessingTime processingTime);

}
