/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.management.stats;

import org.mule.api.annotation.NoImplement;

/**
 * Maintains different statistics for {@link org.mule.runtime.core.api.construct.FlowConstruct} instances
 */
@NoImplement
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
   * Indicates that a new message was dispatched from a message source
   */
  void incMessagesDispatched();

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
   * @return the number of messages dispatched from a source of a flow at a given time
   * 
   * @since 4.5
   */
  long getTotalDispatchedMessages();

  /**
   * @return  indicates if the statistic is enabled or not.
   */
  void setEnabled(boolean b);

  /**
   * Resets all the statistic state
   */
  void clear();

  /**
   * Provides a counter for {@link #getExecutionErrors() execution errors} that is not affected by calls to {@link #clear()} or
   * {@link ResetOnQueryCounter#getAndReset()} calls to other instances returned by this method.
   * <p>
   * Counter initial value is set to the value of {@link #getExecutionErrors()} when this method is called.
   * <p>
   * If this is called concurrently with {@link #incExecutionError()}, there is chance of a race condition occurring where an
   * event may be counted twice. To avoid this possibility, get the counters before statistics begin to be populated.
   * 
   * @return a counter for {@link #getExecutionErrors()}.
   * 
   * @since 4.5
   */
  ResetOnQueryCounter getExecutionErrorsCounter();

  /**
   * Provides a counter for {@link #getFatalErrors() fatal errors} that is not affected by calls to {@link #clear()} or
   * {@link ResetOnQueryCounter#getAndReset()} calls to other instances returned by this method.
   * <p>
   * Counter initial value is set to the value of {@link #getFatalErrors()} when this method is called.
   * <p>
   * If this is called concurrently with {@link #incFatalError()}, there is chance of a race condition occurring where an event
   * may be counted twice. To avoid this possibility, get the counters before statistics begin to be populated.
   * 
   * @return a counter for {@link #getFatalErrors()}.
   * 
   * @since 4.5
   */
  ResetOnQueryCounter getFatalErrorsCounter();

  /**
   * Provides a counter for {@link #getTotalEventsReceived() total events received} that is not affected by calls to
   * {@link #clear()} or {@link ResetOnQueryCounter#getAndReset()} calls to other instances returned by this method.
   * <p>
   * Counter initial value is set to the value of {@link #getTotalEventsReceived()} when this method is called.
   * <p>
   * If this is called concurrently with {@link #incReceivedEvents()}, there is chance of a race condition occurring where an
   * event may be counted twice. To avoid this possibility, get the counters before statistics begin to be populated.
   * 
   * @return a counter for {@link #getTotalEventsReceived()}.
   * 
   * @since 4.5
   */
  ResetOnQueryCounter getEventsReceivedCounter();

  /**
   * Provides a counter for {@link #getTotalDispatchedMessages() total dispatched messages} that is not affected by calls to
   * {@link #clear()} or {@link ResetOnQueryCounter#getAndReset()} calls to other instances returned by this method.
   * <p>
   * Counter initial value is set to the value of {@link #getTotalDispatchedMessages()} when this method is called.
   * <p>
   * If this is called concurrently with {@link #incReceivedEvents()}, there is chance of a race condition occurring where an
   * event may be counted twice. To avoid this possibility, get the counters before statistics begin to be populated.
   * 
   * @return a counter for {@link #getTotalDispatchedMessages()}.
   * 
   * @since 4.5
   */
  ResetOnQueryCounter getDispatchedMessagesCounter();

}
