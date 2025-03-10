/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static java.util.Collections.singletonList;
import org.junit.Test;

import java.util.List;

public class RouterStatisticsTestCase {

  @Test
  public void increments() {
    RouterStatistics stats = new RouterStatistics(RouterStatistics.TYPE_OUTBOUND);
    stats.incrementCaughtMessage();
    stats.incrementNoRoutedMessage();
    stats.incrementCaughtMessage();
    stats.incrementNoRoutedMessage();
    stats.incrementCaughtMessage();
    assertThat(stats.getNotRouted(), is(2L));
    assertThat(stats.getCaughtMessages(), is(3L));
    assertThat(stats.getTotalReceived(), is(2L));
    assertThat(stats.getTotalRouted(), is(0L));
    stats.clear();
    assertThat(stats.getNotRouted(), is(0L));
    assertThat(stats.getCaughtMessages(), is(0L));
    assertThat(stats.getTotalReceived(), is(0L));
    stats.incrementNoRoutedMessage();
    assertThat(stats.getNotRouted(), is(1L));
    assertThat(stats.getTotalReceived(), is(1L));
    assertThat(stats.isInbound(), is(false));
    assertThat(stats.isEnabled(), is(false));
    stats.setEnabled(true);
    assertThat(stats.isEnabled(), is(true));
  }

  @Test
  public void routeMessage() {
    RouterStatistics stats = new RouterStatistics(RouterStatistics.TYPE_OUTBOUND);
    List<String> route = singletonList("someRoute");
    stats.incrementRoutedMessage(route);
    assertThat(stats.getTotalRouted(), is(1L));
    assertThat(stats.getTotalReceived(), is(1L));
    assertThat(stats.getNotRouted(), is(0L));
  }

}
