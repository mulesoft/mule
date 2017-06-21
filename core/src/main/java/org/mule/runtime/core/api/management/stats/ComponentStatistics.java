/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import org.mule.runtime.core.api.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ComponentStatistics is a basic metrics aggregation class that is accessible via the JMX api. This class is not thread-safe -
 * occasional errors in reported statistics should be expected, especially when the {@link #clear()} method is used.
 */
public class ComponentStatistics implements Statistics {

  private static final Logger logger = LoggerFactory.getLogger(ComponentStatistics.class);

  /**
   * Serial version
   */
  private static final long serialVersionUID = -2086999226732861674L;

  private long minExecutionTime = 0;
  private long maxExecutionTime = 0;
  private long averageExecutionTime = 0;
  private long executedEvent = 0;
  private long totalExecTime = 0;
  private boolean enabled = false;
  private long intervalTime = 0;
  private long currentIntervalStartTime = 0;
  private boolean statIntervalTimeEnabled = false;

  /**
   * The constructor added to initialize the interval time in ms that stats are measured for from the property statIntervalTime.
   * If the property is not set or cannot be parsed, disable interval time and just compute stats from start of mule.
   * <p/>
   * TODO: The code to create and use an interval time for measuring average execution time could be removed once a complete
   * solution is available in MuleHQ to monitor this
   */
  public ComponentStatistics() {
    String intervalTimeString = System.getProperty("statIntervalTime");
    if (StringUtils.isBlank(intervalTimeString)) {
      statIntervalTimeEnabled = false;
    } else {
      try {
        intervalTime = Integer.parseInt(intervalTimeString);
        statIntervalTimeEnabled = true;
      } catch (NumberFormatException e) {
        // just disable it
        statIntervalTimeEnabled = false;
        logger.warn("Couldn't parse statIntervalTime: " + intervalTimeString + ". Disabled.");
      }
    }
  }

  /**
   * Resets the state of this component statistics collector.
   * <p/>
   * If called while a branch is being executed, then statistics may be slightly erroneous.
   */
  public void clear() {
    minExecutionTime = 0;
    maxExecutionTime = 0;
    executedEvent = 0;
    totalExecTime = 0;
    averageExecutionTime = 0;
  }

  /**
   * Returns true if this stats collector is enabled.
   * <p/>
   * This value does not affect statistics tabulation directly - it is up to the component to enable/disable collection based on
   * the value of this method.
   * 
   * @return {@code true} if stats collection is enabled, otherwise false.
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Tags this stats collector as enabled or disabled.
   * <p/>
   * Does not affect stats calculation - it is up to the caller to check this flag.
   * 
   * @param enabled {@code true} if stats should be enabled, otherwise false.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * The maximum total event execution time seen since last cleared.
   * 
   * @return The maximum time, or zero if no events have been started.
   */
  public long getMaxExecutionTime() {
    return maxExecutionTime;
  }

  /**
   * The minimum total event execution time seen since last cleared.
   * 
   * @return The maximum time, or zero if no events have been completed.
   */
  public long getMinExecutionTime() {
    return minExecutionTime;
  }

  /**
   * The total cumulative execution time since statistics were last cleared. Includes the sum of all branch times plus any
   * directly recorded execution times.
   * 
   * @return The total cumulative execution time, in milliseconds.
   */
  public long getTotalExecutionTime() {
    return totalExecTime;
  }

  /**
   * Then number of events executed since last cleared.
   * <p/>
   * NOTE: When branch times are recorded, an event will typically be recorded as 'executed' on the first branch event. See
   * {@link #addExecutionBranchTime(boolean, long, long)}.
   * 
   * @return The number of events executed since last cleared.
   */
  public long getExecutedEvents() {
    return executedEvent;
  }

  /**
   * Add a new execution-time measurement for one branch of processing an event.
   * 
   * @param first true if this is the first branch for this event
   * @param branch the time to execute this branch
   * @param total the total time (so far) for processing this event
   */
  public synchronized void addExecutionBranchTime(boolean first, long branch, long total) {
    // TODO MULE-9151 - ComponentStatistics should really create distinct Event
    // objects that can be used to aggregate statistics and then atomically
    // log them at completion time.

    if (statIntervalTimeEnabled) {
      long currentTime = System.currentTimeMillis();
      if (currentIntervalStartTime == 0) {
        currentIntervalStartTime = currentTime;
      }

      if ((currentTime - currentIntervalStartTime) > intervalTime) {
        clear();
        currentIntervalStartTime = currentTime;
      }
    }

    if (first) {
      executedEvent++;
    }

    if (executedEvent > 0) {
      totalExecTime += ProcessingTime.getEffectiveTime(branch);
      long effectiveTotal = ProcessingTime.getEffectiveTime(total);
      if (maxExecutionTime == 0 || effectiveTotal > maxExecutionTime) {
        maxExecutionTime = effectiveTotal;
      }
      averageExecutionTime = totalExecTime / executedEvent;
    }
  }

  /**
   * Add the complete execution time for a flow that also reports branch execution times.
   * <p/>
   * Use in conjunction with {@link #addExecutionBranchTime(boolean, long, long)}.
   * 
   * @param time the total time required to process this event
   */
  public synchronized void addCompleteExecutionTime(long time) {
    if (executedEvent > 0) {
      long effectiveTime = ProcessingTime.getEffectiveTime(time);
      if (minExecutionTime == 0 || effectiveTime < minExecutionTime) {
        minExecutionTime = effectiveTime;
      }
    }
  }

  /**
   * Add a new execution-time measurement for processing an event.
   * <p/>
   * Do not use when reporting branch execution times; instead see {@link #addCompleteExecutionTime(long)}.
   *
   * @param time The total event time to be logged/recorded.
   */
  public synchronized void addExecutionTime(long time) {
    if (statIntervalTimeEnabled) {
      long currentTime = System.currentTimeMillis();
      if (currentIntervalStartTime == 0) {
        currentIntervalStartTime = currentTime;
      }

      if ((currentTime - currentIntervalStartTime) > intervalTime) {
        clear();
        currentIntervalStartTime = currentTime;
      }
    }

    executedEvent++;

    long effectiveTime = ProcessingTime.getEffectiveTime(time);
    totalExecTime += effectiveTime;

    if (minExecutionTime == 0 || effectiveTime < minExecutionTime) {
      minExecutionTime = time;
    }
    if (maxExecutionTime == 0 || effectiveTime > maxExecutionTime) {
      maxExecutionTime = time;
    }
    averageExecutionTime = totalExecTime / executedEvent;
  }

  /**
   * Returns the average execution time, rounded downwards.
   * 
   * @return the total event time accumulated to this point, divided by the total number of events recorded.
   */
  public long getAverageExecutionTime() {
    return averageExecutionTime;
  }

}
