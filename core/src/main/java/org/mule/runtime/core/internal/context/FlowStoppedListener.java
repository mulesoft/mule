/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.context;

public interface FlowStoppedListener {

  /**
   * Notifies the starting of a flow.
   */
  void onStart();

  /**
   * Notifies the stopping of a flow.
   */
  void onStop();

  /**
   * Selects persistance of the flow's state
   */
  void doNotPersist();

  /**
   * Checks if the flow should start or not
   */
  void checkIfFlowShouldStart();

  /**
   * Indicates if the flow should start or not
   */
  Boolean shouldStart();
}
