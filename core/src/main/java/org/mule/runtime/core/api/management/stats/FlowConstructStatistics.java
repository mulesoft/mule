/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.management.stats;

/**
 * Maintains different statistics for {@link org.mule.runtime.core.api.construct.FlowConstruct} instances
 */
public interface FlowConstructStatistics extends Statistics {

  /**
   * @return the name given to the statistic
   */
  String getName();

  /**
   * Indicates that a new event was received
   */
  void incReceivedEvents();

  /**
   * Indicates that an execution error has ocurred
   */
  void incExecutionError();

  /**
   * Indicates that a fatal error has ocurred
   */
  void incFatalError();

  /**
   * Adds the execution time of a processed event
   */
  void addCompleteFlowExecutionTime(long time);

  /**
   * Adds the execution time of a processed event
   */
  void addFlowExecutionBranchTime(long time, long total);

  long getAverageProcessingTime();

  long getProcessedEvents();

  /**
   * @return the maximum time required to process an event
   */
  long getMaxProcessingTime();

  /**
   * @return the minimum time required to process an event
   */
  long getMinProcessingTime();

  /**
   * @return the time consumed to process all the current finalized events
   */
  long getTotalProcessingTime();

  /**
   * @return the number of execution errors at a given time
   */
  long getExecutionErrors();

  /**
   * @return the number of fatal errors at a given time
   */
  long getFatalErrors();

  /**
   * @return the number of event received at a given time
   */
  long getTotalEventsReceived();

  /**
   * @return  indicates if the statistic is enabled or not.
   */
  void setEnabled(boolean b);

  /**
   * Resets all the statistic state
   */
  void clear();
}
