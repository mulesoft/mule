/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_BACKPRESSURE_TRIGGERED;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.BACKPRESSURE;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SUPPORTABILITY;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SupportabilityStory.ALERTS;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.IsIterableContaining.hasItem;

import org.mule.runtime.api.alert.AlertingSupport;
import org.mule.runtime.api.alert.TimedDataAggregation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.alert.MuleAlertingSupport;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.extension.api.runtime.source.BackPressureContext;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import jakarta.inject.Inject;

@Features({@Feature(SOURCES), @Feature(SUPPORTABILITY)})
@Stories({@Story(BACKPRESSURE), @Story(ALERTS)})
public class BackPressureTestCase extends AbstractExtensionFunctionalTestCase {

  private static List<CoreEvent> EVENTS;

  public static class Collector implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (EVENTS) {
        EVENTS.add(event);
        return event;
      }
    }
  }

  @Inject
  private AlertingSupport alertingSupport;

  private HeisenbergExtension heisenberg;
  private List<BackPressureContext> backPressureContexts;
  private List<org.mule.sdk.api.runtime.source.BackPressureContext> sdkBackPressureContexts;

  @Override
  protected String getConfigFile() {
    return "source/heisenberg-backpressure-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    heisenberg = getConfigurationFromRegistry("heisenberg", testEvent(), muleContext);
    assertThat(heisenberg, is(notNullValue()));
    backPressureContexts = new LinkedList<>();
    sdkBackPressureContexts = new LinkedList<>();
    EVENTS = new LinkedList<>();
  }

  @Override
  protected void doTearDown() throws Exception {
    backPressureContexts = null;
    sdkBackPressureContexts = null;
    EVENTS = null;
  }

  @Test
  public void backPressureWithFailStrategy() throws Exception {
    startFlow("defaultToFail");
    try {
      check(15000, 100, () -> {
        sdkBackPressureContexts.addAll(heisenberg.getSdkBackPressureContexts());
        return !sdkBackPressureContexts.isEmpty();
      });

      org.mule.sdk.api.runtime.source.BackPressureContext sample = sdkBackPressureContexts.get(0);
      assertThat(sample.getAction(), is(org.mule.sdk.api.runtime.source.BackPressureAction.FAIL));
      assertThat(sample.getEvent().getMessage().getPayload().getValue().toString(), containsString("If found by DEA contact"));
      assertThat(sample.getSourceCallbackContext(), is(notNullValue()));
    } finally {
      stopFlow("defaultToFail");
    }

    var alertsAggregation = aggregateAlerts();
    assertThat(alertsAggregation, hasKey(ALERT_BACKPRESSURE_TRIGGERED));
    assertThat(alertsAggregation.get(ALERT_BACKPRESSURE_TRIGGERED).forLast15MinsInterval(),
               hasItem("MAX_CONCURRENCY_EXCEEDED - defaultToFail"));
  }

  @Test
  public void backPressureWithDropStrategy() throws Exception {
    startFlow("configuredToDrop");
    try {
      check(15000, 100, () -> {
        sdkBackPressureContexts.addAll(heisenberg.getSdkBackPressureContexts());
        return !sdkBackPressureContexts.isEmpty();
      });

      org.mule.sdk.api.runtime.source.BackPressureContext sample = sdkBackPressureContexts.get(0);
      assertThat(sample.getAction(), is(org.mule.sdk.api.runtime.source.BackPressureAction.DROP));
      assertThat(sample.getEvent().getMessage().getPayload().getValue().toString(), containsString("If found by DEA contact"));
      assertThat(sample.getSourceCallbackContext(), is(notNullValue()));
    } finally {
      stopFlow("configuredToDrop");
    }

    var alertsAggregation = aggregateAlerts();
    assertThat(alertsAggregation, hasKey(ALERT_BACKPRESSURE_TRIGGERED));
    assertThat(alertsAggregation.get(ALERT_BACKPRESSURE_TRIGGERED).forLast15MinsInterval(),
               hasItem("MAX_CONCURRENCY_EXCEEDED - configuredToDrop"));
  }

  @Test
  public void defaultToWait() throws Exception {
    startFlow("defaultCase");
    try {
      check(15000, 100, () -> EVENTS.size() >= 3);

      assertThat(backPressureContexts, hasSize(0));
    } finally {
      stopFlow("defaultCase");
    }

    var alertsAggregation = aggregateAlerts();
    assertThat(alertsAggregation, hasKey(ALERT_BACKPRESSURE_TRIGGERED));
    assertThat(alertsAggregation.get(ALERT_BACKPRESSURE_TRIGGERED).forLast15MinsInterval(),
               hasItem("MAX_CONCURRENCY_EXCEEDED - defaultCase"));
  }

  private Map<String, TimedDataAggregation<Set<String>>> aggregateAlerts() {
    return ((MuleAlertingSupport) alertingSupport).alertsAggregation(() -> new TreeSet<>(), (a, t) -> {
      a.add(Objects.toString(t, "(null)"));
      return a;
    });
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) registry.lookupByName(flowName).get()).start();
  }

  private void stopFlow(String flowName) throws Exception {
    ((Stoppable) registry.lookupByName(flowName).get()).stop();
  }

}
