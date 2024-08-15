/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.context;

/**
 * Defines a listener to persist stop events of Mule flows.
 *
 * @since 4.2.3 4.3.1 4.4.0
 */
public interface FlowStoppedPersistenceListener {

  /**
   * Notifies the starting of a flow.
   */
  void onStart();

  /**
   * Notifies the stopping of a flow.
   */
  void onStop();

  /**
   * Turns off persistence.
   * <p>
   * The flow's stopped state should only be persisted if it was stopped by external users. Since external users usually call the
   * flow's stop() method directly from their own methods, a workaround is to prevent persistence when the flow is stopped for
   * other reasons.
   */
  void doNotPersist();

  /**
   * Indicates if the flow should start or not
   */
  Boolean shouldStart();
}
