/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.metrics.api.meter.MeterProperties.MULE_METER_ARTIFACT_ID_ATTRIBUTE;

import org.mule.runtime.core.api.management.stats.ArtifactMeterProvider;
import org.mule.runtime.core.api.management.stats.FlowsSummaryStatistics;
import org.mule.runtime.metrics.api.meter.Meter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @since 4.5
 */
public class DefaultFlowsSummaryStatistics implements FlowsSummaryStatistics {

  private static final long serialVersionUID = 1L;
  public static final String FLOWS_SUMMARY_APP_STATISTICS_NAME = "flows-summary-statistic";
  public static final String FLOWS_SUMMARY_APP_STATISTICS_DESCRIPTION_TEMPLATE = "Flow summary statistics";
  public static final String DECLARED_PRIVATE_FLOWS_APP_NAME = "declared-private-flows";
  public static final String DECLARED_PRIVATE_FLOWS_APP_DESCRIPTION = "Declared Private Flows";
  public static final String ACTIVE_PRIVATE_FLOWS_APP_NAME = "active-private-flows";
  public static final String ACTIVE_PRIVATE_FLOWS_APP_DESCRIPTION = "Activate Private Flows";
  public static final String DECLARED_TRIGGER_FLOWS_APP_NAME = "declared-trigger-flows";
  public static final String DECLARED_TRIGGER_FLOWS_APP_DESCRIPTION = "Declared Trigger Flows";
  public static final String ACTIVE_TRIGGER_FLOWS_NAME = "active-trigger-flows";
  public static final String ACTIVE_TRIGGER_FLOWS_DESCRIPTION = "Active Trigger Flows";
  public static final String DECLARED_APIKIT_FLOWS_APP_NAME = "declared-apikit-flows";
  public static final String DECLARED_APIKIT_FLOWS_APP_DESCRIPTION = "Declared ApiKit Flows";
  public static final String ACTIVE_APIKIT_FLOWS_APP_NAME = "active-apikit-flows";
  public static final String ACTIVE_APIKIT_FLOWS_APP_DESCRIPTION = "Active Apikit Flows";

  private final boolean enabled;

  private final AtomicInteger declaredPrivateFlows = new AtomicInteger(0);
  private final AtomicInteger activePrivateFlows = new AtomicInteger(0);
  private final AtomicInteger declaredTriggerFlows = new AtomicInteger(0);
  private final AtomicInteger activeTriggerFlows = new AtomicInteger(0);
  private final AtomicInteger declaredApikitFlows = new AtomicInteger(0);
  private final AtomicInteger activeApikitFlows = new AtomicInteger(0);
  private final Set<String> declaredPrivateFlowNames = Collections.synchronizedSet(new HashSet<>());
  private final Set<String> declaredTriggerFlowNames = Collections.synchronizedSet(new HashSet<>());
  private final Set<String> declaredApiKitFlowNames = Collections.synchronizedSet(new HashSet<>());

  public DefaultFlowsSummaryStatistics(boolean isStatisticsEnabled) {
    this.enabled = isStatisticsEnabled;
  }

  /**
   * This object is meant to reflect a state at a given point in time rather than count things that happen in a time window. It is
   * not possible to reconstruct the current state when this is enabled if previous events have been missed.
   * <p>
   * So, its initial enabled state will be immutable.
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public int incrementDeclaredPrivateFlow(String flowName) {
    declaredPrivateFlowNames.add(flowName);
    return declaredPrivateFlows.incrementAndGet();
  }

  public int incrementActivePrivateFlow() {
    return activePrivateFlows.incrementAndGet();
  }

  public int incrementDeclaredTriggerFlow(String flowName) {
    declaredTriggerFlowNames.add(flowName);
    return declaredTriggerFlows.incrementAndGet();
  }

  public int incrementActiveTriggerFlow() {
    return activeTriggerFlows.incrementAndGet();
  }

  public int incrementDeclaredApikitFlow(String flowName) {
    declaredApiKitFlowNames.add(flowName);
    return declaredApikitFlows.incrementAndGet();
  }

  public int incrementActiveApikitFlow() {
    return activeApikitFlows.incrementAndGet();
  }

  public int decrementActiveTriggerFlow() {
    return activeTriggerFlows.decrementAndGet();
  }

  public int decrementActivePrivateFlow() {
    return activePrivateFlows.decrementAndGet();
  }

  public int decrementActiveApikitFlow() {
    return activeApikitFlows.decrementAndGet();
  }

  public int decrementDeclaredPrivateFlow(String flowName) {
    declaredPrivateFlowNames.remove(flowName);
    return declaredPrivateFlows.decrementAndGet();
  }

  public int decrementDeclaredTriggerFlow(String flowName) {
    declaredTriggerFlowNames.remove(flowName);
    return declaredTriggerFlows.decrementAndGet();
  }

  public int decrementDeclaredApikitFlow(String flowName) {
    declaredApiKitFlowNames.remove(flowName);
    return declaredApikitFlows.decrementAndGet();
  }

  @Override
  public int getDeclaredPrivateFlows() {
    return declaredPrivateFlows.get();
  }

  @Override
  public int getActivePrivateFlows() {
    return activePrivateFlows.get();
  }

  @Override
  public int getDeclaredTriggerFlows() {
    return declaredTriggerFlows.get();
  }

  @Override
  public int getActiveTriggerFlows() {
    return activeTriggerFlows.get();
  }

  @Override
  public int getDeclaredApikitFlows() {
    return declaredApikitFlows.get();
  }

  @Override
  public int getActiveApikitFlows() {
    return activeApikitFlows.get();
  }

  @Override
  public void trackUsingMeterProvider(ArtifactMeterProvider meterProvider) {
    String artifactId = meterProvider.getArtifactId();
    Meter meter = meterProvider.getMeterBuilder(FLOWS_SUMMARY_APP_STATISTICS_NAME)
        .withDescription(FLOWS_SUMMARY_APP_STATISTICS_DESCRIPTION_TEMPLATE)
        .withMeterAttribute(MULE_METER_ARTIFACT_ID_ATTRIBUTE, artifactId)
        .build();

    // Register the declared private flows.
    meter.counterBuilder(DECLARED_PRIVATE_FLOWS_APP_NAME + getMetricSuffix())
        .withValueSupplier(() -> (long) declaredPrivateFlows.get())
        .withAddOperation((value, context) -> declaredPrivateFlows.addAndGet(value.intValue()))
        .withIncrementAndGetOperation(stringStringMap -> (long) declaredPrivateFlows.incrementAndGet())
        .withDescription(DECLARED_PRIVATE_FLOWS_APP_DESCRIPTION).build();


    // Register the active private flows.
    meter.counterBuilder(ACTIVE_PRIVATE_FLOWS_APP_NAME + getMetricSuffix())
        .withValueSupplier(() -> (long) activePrivateFlows.get())
        .withAddOperation((value, context) -> activePrivateFlows.addAndGet(value.intValue()))
        .withIncrementAndGetOperation(stringStringMap -> (long) activePrivateFlows.incrementAndGet())
        .withDescription(ACTIVE_PRIVATE_FLOWS_APP_DESCRIPTION).build();

    // Register the declared trigger flows.
    meter.counterBuilder(DECLARED_TRIGGER_FLOWS_APP_NAME + getMetricSuffix())
        .withValueSupplier(() -> (long) declaredTriggerFlows.get())
        .withAddOperation((value, context) -> declaredTriggerFlows.addAndGet(value.intValue()))
        .withIncrementAndGetOperation(stringStringMap -> (long) declaredTriggerFlows.incrementAndGet())
        .withDescription(DECLARED_TRIGGER_FLOWS_APP_DESCRIPTION).build();


    // Register the active trigger flows.
    meter.counterBuilder(ACTIVE_TRIGGER_FLOWS_NAME + getMetricSuffix())
        .withValueSupplier(() -> (long) activeTriggerFlows.get())
        .withAddOperation((value, context) -> activeTriggerFlows.addAndGet(value.intValue()))
        .withIncrementAndGetOperation(stringStringMap -> (long) activeTriggerFlows.incrementAndGet())
        .withDescription(ACTIVE_TRIGGER_FLOWS_DESCRIPTION).build();

    // Register the declared apikit flows.
    meter.counterBuilder(DECLARED_APIKIT_FLOWS_APP_NAME + getMetricSuffix())
        .withValueSupplier(() -> (long) declaredApikitFlows.get())
        .withAddOperation((value, context) -> declaredApikitFlows.addAndGet(value.intValue()))
        .withIncrementAndGetOperation(stringStringMap -> (long) declaredApikitFlows.incrementAndGet())
        .withDescription(DECLARED_APIKIT_FLOWS_APP_DESCRIPTION).build();

    // Register the active apikit flows.
    meter.counterBuilder(ACTIVE_APIKIT_FLOWS_APP_NAME + getMetricSuffix())
        .withValueSupplier(() -> (long) activeApikitFlows.get())
        .withAddOperation((value, context) -> activeApikitFlows.addAndGet(value.intValue()))
        .withIncrementAndGetOperation(stringStringMap -> (long) activeApikitFlows.incrementAndGet())
        .withDescription(ACTIVE_APIKIT_FLOWS_APP_DESCRIPTION).build();
  }

  @Override
  public String toString() {
    return "\nFlows Summary: {\n" +
        "\tFlows with event sources: {\n" +
        "\t\tTotal: " + declaredTriggerFlowNames.size() + ",\n" +
        "\t\tFlows: " + declaredTriggerFlowNames + "\n" +
        "\t},\n" +
        "\tNumber of implementation flows generated by APIKit: {\n" +
        "\t\tTotal: " + declaredApiKitFlowNames.size() + ",\n" +
        "\t\tFlows: " + declaredApiKitFlowNames + "\n" +
        "\t}\n" +
        '}';
  }

  // TODO W-18668900: remove once the pilot is concluded
  protected String getMetricSuffix() {
    return "";
  }
}
