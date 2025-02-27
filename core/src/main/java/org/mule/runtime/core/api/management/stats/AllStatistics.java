/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import static org.mule.runtime.api.config.MuleRuntimeFeature.COMPUTE_CONNECTION_ERRORS_IN_STATS;
import static org.mule.runtime.api.meta.MuleVersion.v4_4_0;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyMap;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.internal.management.stats.ApplicationStatistics;
import org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics;
import org.mule.runtime.metrics.api.MeterProvider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>AllStatistics</code> TODO
 */
@NoExtend
public class AllStatistics {

  private boolean isStatisticsEnabled = getBoolean(MULE_ENABLE_STATISTICS);

  private long startTime;
  private final ApplicationStatistics appStats;
  private final FlowsSummaryStatistics flowSummaryStatistics;
  private final Map<String, FlowConstructStatistics> flowConstructStats = new HashMap<>();
  private final Map<String, PayloadStatistics> payloadStatistics = emptyMap();
  private ArtifactMeterProvider meterProvider;

  /**
   *
   */
  public AllStatistics() {
    clear();
    appStats = new ApplicationStatistics(this);
    flowSummaryStatistics = new DefaultFlowsSummaryStatistics(isStatisticsEnabled);
    appStats.setEnabled(isStatisticsEnabled);
    add(appStats);
  }

  public synchronized void clear() {
    for (FlowConstructStatistics statistics : getServiceStatistics()) {
      statistics.clear();
    }
    startTime = currentTimeMillis();
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
   *
   * @deprecated since 4.4.1, 4.5.0. Payload statistics are no longer supported, this method does nothing.
   */
  @Experimental
  @Deprecated(since = "4.5")
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
      if (meterProvider != null) {
        stat.trackUsingMeterProvider(meterProvider);
      }
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

  public FlowsSummaryStatistics getFlowSummaryStatistics() {
    return flowSummaryStatistics;
  }

  /**
   * @return the available payload statistics for all components.
   * @since 4.4, 4.3.1
   * @deprecated since 4.4.1, 4.5.0. Payload statistics are no longer supported and will always return empty data.
   */
  @Experimental
  @Deprecated(since = "4.5")
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
  @Deprecated(since = "4.5")
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
  @Deprecated(since = "4.5")
  public PayloadStatistics getPayloadStatistics(String componentLocation) {
    return payloadStatistics.get(componentLocation);
  }

  /**
   * @return whether the payload statistics are enabled
   * @since 4.4, 4.3.1
   * @deprecated since 4.4.1, 4.5.0. Payload statistics are no longer supported and will always return false.
   */
  @Experimental
  @Deprecated(since = "4.5")
  public boolean isPayloadStatisticsEnabled() {
    return false;
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
                                                    .filter(muleVersion -> muleVersion.atLeast(v4_4_0))
                                                    .isPresent());
  }

  /**
   * Tracks the statistics using the provided {@link MeterProvider} with the corresponding artifact id.
   *
   * @param meterProvider the {@link MeterProvider} to track the statistics
   * @param artifactId    the artifact id.
   */
  public void trackUsingMeterProvider(MeterProvider meterProvider, String artifactId) {
    this.meterProvider = new ArtifactMeterProvider(meterProvider, artifactId);
    this.flowSummaryStatistics.trackUsingMeterProvider(this.meterProvider);
    this.flowConstructStats.values()
        .forEach(flowConstructStatsValue -> flowConstructStatsValue.trackUsingMeterProvider(this.meterProvider));
  }
}
