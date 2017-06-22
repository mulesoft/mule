/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.runtime.core.api.management.stats.ComponentStatistics;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

/**
 * Validates some basic assumptions about the ComponentStatistics class behavior.
 */
public class ComponentStatisticsTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemProperty statIntervalTime = new SystemProperty("statIntervalTime", null);

  private static void assertValues(ComponentStatistics stats, long numEvents, long totalTime, long avgTime, long maxTime,
                                   long minTime) {
    assertThat("getExecutedEvents", stats.getExecutedEvents(), equalTo(numEvents));
    assertThat("getTotalExecutionTime", stats.getTotalExecutionTime(), equalTo(totalTime));
    assertThat("getAverageExecutionTime", stats.getAverageExecutionTime(), equalTo(avgTime));
    assertThat("getMaxExecutionTime", stats.getMaxExecutionTime(), equalTo(maxTime));
    assertThat("getMinExecutionTime", stats.getMinExecutionTime(), equalTo(minTime));
  }

  @Test
  public void verifyStatDefaults() {
    ComponentStatistics stats = new ComponentStatistics();
    assertValues(stats, 0L, 0L, 0L, 0L, 0L);
    assertThat(stats.isEnabled(), equalTo(false));
  }

  @Test
  public void processSingleEvent() {
    ComponentStatistics stats = new ComponentStatistics();
    stats.addExecutionTime(100L);
    assertValues(stats, 1L, 100L, 100L, 100L, 100L);
  }

  @Test
  public void processSingleBranchEvent() {
    ComponentStatistics stats = new ComponentStatistics();
    stats.addExecutionBranchTime(true, 25L, 25L);
    assertValues(stats, 1L, 25L, 25L, 25L, 0L);
    stats.addExecutionBranchTime(false, 25L, 50L);
    assertValues(stats, 1L, 50L, 50L, 50L, 0L);
    stats.addCompleteExecutionTime(50L);
    assertValues(stats, 1L, 50L, 50L, 50L, 50L);
  }

  @Test
  public void clearStats() {
    ComponentStatistics stats = new ComponentStatistics();
    stats.addExecutionTime(100L);
    stats.clear();
    assertValues(stats, 0L, 0L, 0L, 0L, 0L);
  }

  /**
   * New behavior under the fix to MULE-6417 - no longer throws a divide-by-zero error. Instead, the remainder of the fragmented
   * event is ignored until a new event is started.
   * <p/>
   * Note that this is a partial solution - if multiple components are active at the same time, collection can be 're-enabled' for
   * an already-started event. The established API does not allow for a solution, so for now this quirk must be accepted.
   */
  @Test
  public void clearDuringBranch() {
    ComponentStatistics stats = new ComponentStatistics();
    stats.addExecutionBranchTime(true, 25L, 25L);
    stats.clear();
    assertValues(stats, 0L, 0L, 0L, 0L, 0L);
    stats.addExecutionBranchTime(false, 25L, 50L);
    assertValues(stats, 0L, 0L, 0L, 0L, 0L);
  }

  @Test
  public void verifyMaxMinAverage() {
    ComponentStatistics stats = new ComponentStatistics();
    stats.addExecutionTime(2L);
    stats.addExecutionTime(3L);
    assertValues(stats, 2L, 5L, 2L, 3L, 2L);
  }

  @Test
  public void verifyBranchMaxMinAverage() {
    ComponentStatistics stats = new ComponentStatistics();
    stats.addExecutionBranchTime(true, 2L, 2L);
    stats.addCompleteExecutionTime(2L);
    stats.addExecutionBranchTime(true, 3L, 3L);
    stats.addCompleteExecutionTime(3L);
    assertValues(stats, 2L, 5L, 2L, 3L, 2L);
  }

  @Test
  public void verifyMultiBranchMaxMinAverage() {
    ComponentStatistics stats = new ComponentStatistics();
    stats.addExecutionBranchTime(true, 1L, 1L);
    stats.addExecutionBranchTime(false, 1L, 2L);
    stats.addCompleteExecutionTime(2L);
    stats.addExecutionBranchTime(true, 3L, 3L);
    stats.addCompleteExecutionTime(3L);
    assertValues(stats, 2L, 5L, 2L, 3L, 2L);
  }

  @Test
  public void verifyShortStatIntervalReset() {
    // configure to reset continuously
    // this functionality is flawed in many ways, but we'll test the basics anyways
    System.setProperty("statIntervalTime", "-1");
    ComponentStatistics stats = new ComponentStatistics();

    // single
    // reset and then collect
    stats.addExecutionTime(100L);
    assertValues(stats, 1L, 100L, 100L, 100L, 100L);
    // reset and then collect
    stats.addExecutionTime(200L);
    assertValues(stats, 1L, 200L, 200L, 200L, 200L);

    // branch
    // reset and then collect
    stats.addExecutionBranchTime(true, 100L, 100L);
    assertValues(stats, 1L, 100L, 100L, 100L, 0L);
    // reset and then collect
    stats.addExecutionBranchTime(true, 200L, 200L);
    assertValues(stats, 1L, 200L, 200L, 200L, 0L);
    // currently doesn't reset
    stats.addCompleteExecutionTime(200L);
    assertValues(stats, 1L, 200L, 200L, 200L, 200L);
  }

  @Test
  public void verifyLongStatIntervalNoReset() {
    // configure to reset far into the future
    System.setProperty("statIntervalTime", "9999");
    ComponentStatistics stats = new ComponentStatistics();
    stats.addExecutionTime(100L);
    assertValues(stats, 1L, 100L, 100L, 100L, 100L);
    // no reset expected
    stats.addExecutionBranchTime(true, 100L, 100L);
    assertValues(stats, 2L, 200L, 100L, 100L, 100L);
  }
}
