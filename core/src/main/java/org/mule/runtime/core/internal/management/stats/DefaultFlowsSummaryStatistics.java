/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.metrics.api.meter.MeterProperties.MULE_METER_ARTIFACT_ID_ATTRIBUTE;
import static java.util.regex.Pattern.compile;

import org.mule.runtime.core.api.management.stats.ArtifactMeterProvider;
import org.mule.runtime.core.api.management.stats.FlowsSummaryStatistics;
import org.mule.runtime.metrics.api.meter.Meter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * 
 * @since 4.5
 */
public class DefaultFlowsSummaryStatistics implements FlowsSummaryStatistics {

  private static final String APIKIT_FLOWNAME_REGEX =
      // method
      "(\\w*)" +
      // path
          ":(\\\\[^:]*)" +
          // content type
          "(:[^:]*)?" +
          // config name
          ":([^\\/\\\\\\[\\\\\\]\\{\\}#]*)";
  private static final String APIKIT_SOAP_FLOWNAME_REGEX =
      // method
      "(\\w*)" +
      // path
          ":\\\\" +
          // config name
          "([^\\/\\\\\\[\\\\\\]\\{\\}#]*)";
  private static final Pattern APIKIT_FLOWNAME_PATTERN = compile(APIKIT_FLOWNAME_REGEX);
  private static final Pattern APIKIT_SOAP_FLOWNAME_PATTERN = compile(APIKIT_SOAP_FLOWNAME_REGEX);

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

  public int incrementDeclaredPrivateFlow() {
    return declaredPrivateFlows.incrementAndGet();
  }

  public int incrementActivePrivateFlow() {
    return activePrivateFlows.incrementAndGet();
  }

  public int incrementDeclaredTriggerFlow() {
    return declaredTriggerFlows.incrementAndGet();
  }

  public int incrementActiveTriggerFlow() {
    return activeTriggerFlows.incrementAndGet();
  }

  public int incrementDeclaredApikitFlow() {
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

  public int decrementDeclaredPrivateFlow() {
    return declaredPrivateFlows.decrementAndGet();
  }

  public int decrementDeclaredTriggerFlow() {
    return declaredTriggerFlows.decrementAndGet();
  }

  public int decrementDeclaredApikitFlow() {
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

  /**
   * Determines if the name of a flow follows the conventions of ApiKit.
   * 
   * @param flowName the name of the flow to check.
   * @return whether the name of the flow corresponds to an ApiKit flow.
   */
  public static boolean isApiKitFlow(String flowName) {
    return APIKIT_FLOWNAME_PATTERN.matcher(flowName).matches()
        || APIKIT_SOAP_FLOWNAME_PATTERN.matcher(flowName).matches();
  }

  @Override
  public void trackUsingMeterProvider(ArtifactMeterProvider meterProvider) {
    String artifactId = meterProvider.getArtifactId();
    Meter meter = meterProvider.getMeterBuilder(FLOWS_SUMMARY_APP_STATISTICS_NAME)
        .withDescription(FLOWS_SUMMARY_APP_STATISTICS_DESCRIPTION_TEMPLATE)
        .withMeterAttribute(MULE_METER_ARTIFACT_ID_ATTRIBUTE, artifactId)
        .build();

    // Register the declared private flows.
    meter.counterRegistrationHelper(DECLARED_PRIVATE_FLOWS_APP_NAME)
        .withValueSupplier(() -> (long) declaredPrivateFlows.get())
        .withConsumerForAddOperation(value -> declaredPrivateFlows.addAndGet(value.intValue()))
        .withSupplierForIncrementAndGetOperation(() -> (long) declaredPrivateFlows.incrementAndGet())
        .withDescription(DECLARED_PRIVATE_FLOWS_APP_DESCRIPTION).register();


    // Register the active private flows.
    meter.counterRegistrationHelper(ACTIVE_PRIVATE_FLOWS_APP_NAME)
        .withValueSupplier(() -> (long) activePrivateFlows.get())
        .withConsumerForAddOperation(value -> activePrivateFlows.addAndGet(value.intValue()))
        .withSupplierForIncrementAndGetOperation(() -> (long) activePrivateFlows.incrementAndGet())
        .withDescription(ACTIVE_PRIVATE_FLOWS_APP_DESCRIPTION).register();

    // Register the declared trigger flows.
    meter.counterRegistrationHelper(DECLARED_TRIGGER_FLOWS_APP_NAME)
        .withValueSupplier(() -> (long) declaredTriggerFlows.get())
        .withConsumerForAddOperation(value -> declaredTriggerFlows.addAndGet(value.intValue()))
        .withSupplierForIncrementAndGetOperation(() -> (long) declaredTriggerFlows.incrementAndGet())
        .withDescription(DECLARED_TRIGGER_FLOWS_APP_DESCRIPTION).register();


    // Register the active trigger flows.
    meter.counterRegistrationHelper(ACTIVE_TRIGGER_FLOWS_NAME)
        .withValueSupplier(() -> (long) activeTriggerFlows.get())
        .withConsumerForAddOperation(value -> activeTriggerFlows.addAndGet(value.intValue()))
        .withSupplierForIncrementAndGetOperation(() -> (long) activeTriggerFlows.incrementAndGet())
        .withDescription(ACTIVE_TRIGGER_FLOWS_DESCRIPTION).register();

    // Register the declared apikit flows.
    meter.counterRegistrationHelper(DECLARED_APIKIT_FLOWS_APP_NAME)
        .withValueSupplier(() -> (long) declaredApikitFlows.get())
        .withConsumerForAddOperation(value -> declaredApikitFlows.addAndGet(value.intValue()))
        .withSupplierForIncrementAndGetOperation(() -> (long) declaredApikitFlows.incrementAndGet())
        .withDescription(DECLARED_APIKIT_FLOWS_APP_DESCRIPTION).register();

    // Register the active apikit flows.
    meter.counterRegistrationHelper(ACTIVE_APIKIT_FLOWS_APP_NAME)
        .withValueSupplier(() -> (long) activeApikitFlows.get())
        .withConsumerForAddOperation(value -> activeApikitFlows.addAndGet(value.intValue()))
        .withSupplierForIncrementAndGetOperation(() -> (long) activeApikitFlows.incrementAndGet())
        .withDescription(ACTIVE_APIKIT_FLOWS_APP_DESCRIPTION).register();
  }
}
