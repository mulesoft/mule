/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.config.MuleRuntimeFeature.COMPUTE_CONNECTION_ERRORS_IN_STATS;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.management.stats.ApplicationStatistics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <code>AllStatistics</code> TODO
 */
@NoExtend
public class AllStatistics {

  private boolean isStatisticsEnabled = getBoolean(MULE_ENABLE_STATISTICS);

  private long startTime;
  private final ApplicationStatistics appStats;
  private final Map<String, FlowConstructStatistics> flowConstructStats = new HashMap<>();
  private final Map<String, PayloadStatistics> payloadStatistics = emptyMap();
  private final AtomicInteger declaredPrivateFlows = new AtomicInteger(0);
  private final AtomicInteger activePrivateFlows = new AtomicInteger(0);
  private final AtomicInteger declaredTriggerFlows = new AtomicInteger(0);
  private final AtomicInteger activeTriggerFlows = new AtomicInteger(0);

  /**
   *
   */
  public AllStatistics() {
    clear();
    appStats = new ApplicationStatistics(this);
    appStats.setEnabled(isStatisticsEnabled);
    add(appStats);
  }

  public synchronized void clear() {
    for (FlowConstructStatistics statistics : getServiceStatistics()) {
      statistics.clear();
    }
    startTime = currentTimeMillis();
    declaredPrivateFlows.set(0);
    activePrivateFlows.set(0);
    declaredTriggerFlows.set(0);
    activeTriggerFlows.set(0);
  }

  /**
   * Are statistics logged
   */
  public boolean isEnabled() {
    return isStatisticsEnabled;
  }

  /**
   * Enable statistics logs (this is a dynamic parameter)
   */
  public void setEnabled(boolean enable) {
    isStatisticsEnabled = enable;

    for (FlowConstructStatistics statistics : flowConstructStats.values()) {
      statistics.setEnabled(enable);
    }
  }

  /**
   * @deprecated since 4.4.1, 4.5.0. Payload statistics are no longer supported, this method does nothing.
   */
  @Experimental
  @Deprecated
  public void enablePayloadStatistics(boolean b) {
    // Does nothing.
  }

  public synchronized long getStartTime() {
    return startTime;
  }

  public synchronized void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public synchronized void add(FlowConstructStatistics stat) {
    if (stat != null) {
      stat.setEnabled(isStatisticsEnabled);
      flowConstructStats.put(stat.getName(), stat);
    }
  }

  public synchronized void remove(FlowConstructStatistics stat) {
    if (stat != null) {
      flowConstructStats.remove(stat.getName());
    }
  }

  public synchronized Collection<FlowConstructStatistics> getServiceStatistics() {
    return flowConstructStats.values();
  }

  public FlowConstructStatistics getApplicationStatistics() {
    return appStats;
  }

  /**
   * @return the available payload statistics for all components.
   * @since 4.4, 4.3.1
   * @deprecated since 4.4.1, 4.5.0. Payload statistics are no longer supported and will always return empty data.
   */
  @Experimental
  @Deprecated
  public Collection<PayloadStatistics> getPayloadStatistics() {
    return payloadStatistics.values();
  }

  /**
   * @param component the component to get the statistics for.
   * @return the statistics for the provided {@code component}.
   * @since 4.4, 4.3.1
   * @deprecated since 4.4.1, 4.5.0. Payload statistics are no longer supported and will always return empty data.
   */
  @Experimental
  @Deprecated
  public PayloadStatistics computePayloadStatisticsIfAbsent(Component component) {
    return payloadStatistics.computeIfAbsent(component.getLocation().getLocation(),
        loc -> {
          final PayloadStatistics statistics =
              new PayloadStatistics(loc, component.getIdentifier().toString());
          statistics.setEnabled(isPayloadStatisticsEnabled());
          return statistics;
        });
  }

  /**
   * @param componentLocation the location of the component to get the statistics for.
   * @return the statistics for the component with the provided {@code componentLocation}.
   * @since 4.4, 4.3.1
   * @deprecated since 4.4.1, 4.5.0. Payload statistics are no longer supported and will always return empty data.
   */
  @Experimental
  @Deprecated
  public PayloadStatistics getPayloadStatistics(String componentLocation) {
    return payloadStatistics.get(componentLocation);
  }

  /**
   * @return whether the payload statistics are enabled
   * @since 4.4, 4.3.1
   * @deprecated since 4.4.1, 4.5.0. Payload statistics are no longer supported and will always return false.
   */
  @Experimental
  @Deprecated
  public boolean isPayloadStatisticsEnabled() {
    return false;
  }

  /**
   * Increments the counter of private flows declared in the application. The flow is considered private when it doesn't contain
   * a {@link MessageSource}
   *
   * @return The number of declared private flows
   * @since 4.5.0
   */
  @Experimental
  public int addDeclaredPrivateFlow() {
    return declaredPrivateFlows.incrementAndGet();
  }

  /**
   * Increments the counter of active private flows declared in the application. The flow is considered private when it doesn't
   * contain a {@link MessageSource}.
   * <p>
   * The flow is also considered active when it's in {@code started} state.
   *
   * @return The number of started private flows
   * @since 4.5.0
   */
  @Experimental
  public int addActivePrivateFlow() {
    return activePrivateFlows.incrementAndGet();
  }

  /**
   * Increments the counter of trigger flows declared in the application. Trigger flows are those which include a
   * {@link MessageSource}.
   * <p>
   * The flow is also considered active when it's in {@code started} state.
   *
   * @return The number of declared trigger flows
   * @since 4.5.0
   */
  @Experimental
  public int addDeclaredTriggerFlow() {
    return declaredTriggerFlows.incrementAndGet();
  }

  /**
   * Increments the counter of trigger flows in the application. Trigger flows are those which include a
   * {@link MessageSource}.
   * <p>
   * The flow is also considered active when it's in {@code started} state.
   *
   * @return The number of declared private flows
   * @since 4.5.0
   */
  @Experimental
  public int addActiveTriggerFlow() {
    return activeTriggerFlows.incrementAndGet();
  }

  /**
   * Decrements the active trigger flow counter
   *
   * @return the updated counter value
   * @since 4.5.0
   */
  @Experimental
  public int decrementActiveTriggerFlow() {
    return activeTriggerFlows.addAndGet(-1);
  }

  /**
   * Decrements the active private flow counter
   *
   * @return the updated counter value
   * @since 4.5.0
   */
  @Experimental
  public int decrementActivePrivateFlow() {
    return activePrivateFlows.addAndGet(-1);
  }

  /**
   * Returns the counter of private flows declared in the application. Trigger flows are those which include a
   * {@link MessageSource}.
   *
   * @return The number of declared private flows
   * @since 4.5.0
   */
  @Experimental
  public int getDeclaredPrivateFlows() {
    return declaredPrivateFlows.get();
  }

  /**
   * Returns the counter of trigger flows in the application. Trigger flows are those which include a
   * {@link MessageSource}.
   * <p>
   * The flow is also considered active when it's in {@code started} state.
   *
   * @return The number of declared private flows
   * @since 4.5.0
   */
  @Experimental
  public int getActivePrivateFlows() {
    return activePrivateFlows.get();
  }

  /**
   * Returns the counter of trigger flows declared in the application. Trigger flows are those which include a
   * {@link MessageSource}.
   *
   * @return The number of declared private flows
   * @since 4.5.0
   */
  @Experimental
  public int getDeclaredTriggerFlows() {
    return declaredTriggerFlows.get();
  }

  /**
   * Returns the counter of trigger flows in the application. Trigger flows are those which include a
   * {@link MessageSource}.
   * <p>
   * The flow is also considered active when it's in {@code started} state.
   *
   * @return The number of declared private flows
   * @since 4.5.0
   */
  @Experimental
  public int getActiveTriggerFlows() {
    return activeTriggerFlows.get();
  }

  /**
   * Configures the {@link MuleRuntimeFeature#COMPUTE_CONNECTION_ERRORS_IN_STATS} feature flag.
   *
   * @since 4.4.0, 4.3.1
   */
  public static void configureComputeConnectionErrorsInStats() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(COMPUTE_CONNECTION_ERRORS_IN_STATS,
        featureContext -> featureContext.getArtifactMinMuleVersion()
            .filter(muleVersion -> muleVersion.atLeast("4.4.0")).isPresent());
  }
}
