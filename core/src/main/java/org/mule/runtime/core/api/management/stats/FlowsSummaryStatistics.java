/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.source.MessageSource;

/**
 * Provides summary information about the flows in the deployed application.
 * 
 * <strong>Private/Trigger flows</strong>
 * <ul>
 * <li>A flow is considered <b>trigger</b> when it contains a {@link MessageSource} or it is a flow used by an ApiKit router.</li>
 * <li>A flow that is not <b>trigger</b> is considered <b>private</b>.</li>
 * </ul>
 * <p>
 * <strong>Active/Declared flows</strong>
 * <ul>
 * <li>A flow is also considered <b>active</b> when it's in {@code started} state.</li>
 * <li>A <b>declared</b> flow is a flow that exists in an application, regardless of its lifecycle state (it can be either
 * {@code started} or {@code stopped}).</li>
 * </ul>
 * 
 * @since 4.5
 */
@NoImplement
@Experimental
public interface FlowsSummaryStatistics extends Statistics {

  //////////////////
  // GETTERS
  //////////////////

  /**
   * Returns the counter of private flows declared in the application.
   *
   * @return The number of declared private flows
   */
  int getDeclaredPrivateFlows();

  /**
   * Returns the counter of private flows active in the application.
   *
   * @return The number of active private flows
   */
  int getActivePrivateFlows();

  /**
   * Returns the counter of trigger flows declared in the application.
   * 
   * @return The number of declared trigger flows
   */
  int getDeclaredTriggerFlows();

  /**
   * Returns the counter of trigger flows active in the application.
   *
   * @return The number of active trigger flows
   */
  int getActiveTriggerFlows();

}
