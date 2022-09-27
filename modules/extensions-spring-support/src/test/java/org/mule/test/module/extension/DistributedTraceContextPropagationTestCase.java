/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.module.extension;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class DistributedTraceContextPropagationTestCase extends AbstractExtensionFunctionalTestCase {

  private static final List<CoreEvent> EVENTS = new LinkedList<>();

  public static final String TRACE_CONTEXT_PROPAGATION = "traceContextPropagation";
  public static final String TRACE_CONTEXT_PROPAGATION_THROUGH_HELPER = "traceContextPropagationThroughHelper";
  public static final String TRACE_CONTEXT_PROPAGATION_THROUGH_HELPER_SDK_API_CORRELATION_INFO =
      "traceContextPropagationThroughHelperSdkApi";

  public static final String TRACE_CONTEXT_PROPAGATION_LEGACY_CALLBACK = "traceContextPropagationLegacyCallback";
  public static final String TRACE_CONTEXT_PROPAGATION_LEGACY_CALLBACK_THROUGH_HELPER =
      "traceContextPropagationLegacyCallbackThroughHelper";
  public static final String TRACE_CONTEXT_PROPAGATION_LEGACY_CALLBACK_THROUGH_HELPER_SDK_API_CORRELATION_INFO =
      "traceContextPropagationLegacyCallbackThroughHelperSdkApiCorrelationInfo";

  private static final long PROBER_TIMEOUT = 15000;
  private static final long PROBER_FREQUENCY = 1000;

  @Override
  protected String getConfigFile() {
    return "distributed-trace-context-propagation-test.xml";
  }

  @Override
  protected void doTearDown() throws Exception {
    EVENTS.clear();
  }

  @Test
  public void defaultTraceContextPropagatorThroughImplicitParameter() throws Exception {
    startFlow(TRACE_CONTEXT_PROPAGATION);
    checkEventProcessed();
    assertEventPayload(EVENTS);
  }

  @Test
  public void defaultTraceContextPropagatorThroughForwardCompatibilityHelper() throws Exception {
    startFlow(TRACE_CONTEXT_PROPAGATION_THROUGH_HELPER);
    checkEventProcessed();
    assertEventPayload(EVENTS);
  }

  @Test
  public void defaultTraceContextPropagatorThroughForwardCompatibilityHelperWithSdkApiCorrelationInfo() throws Exception {
    startFlow(TRACE_CONTEXT_PROPAGATION_THROUGH_HELPER_SDK_API_CORRELATION_INFO);
    checkEventProcessed();
    assertEventPayload(EVENTS);
  }

  @Test
  public void defaultTraceContextPropagatorThroughImplicitParameterWithLegacySourceCallback() throws Exception {
    startFlow(TRACE_CONTEXT_PROPAGATION_LEGACY_CALLBACK);
    checkEventProcessed();
    assertEventPayload(EVENTS);
  }

  @Test
  public void defaultTraceContextPropagatorThroughForwardCompatibilityHelperWithLegacySourceCallback() throws Exception {
    startFlow(TRACE_CONTEXT_PROPAGATION_LEGACY_CALLBACK_THROUGH_HELPER);
    checkEventProcessed();
    assertEventPayload(EVENTS);
  }

  @Test
  public void defaultTraceContextPropagatorThroughForwardCompatibilityHelperWithLegacySourceCallbackWithSdkApiCorrelationInfo()
      throws Exception {
    startFlow(TRACE_CONTEXT_PROPAGATION_LEGACY_CALLBACK_THROUGH_HELPER_SDK_API_CORRELATION_INFO);
    checkEventProcessed();
    assertEventPayload(EVENTS);
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void checkEventProcessed() {
    check(PROBER_TIMEOUT, PROBER_FREQUENCY, () -> {
      synchronized (EVENTS) {
        return EVENTS.size() == 1;
      }
    });
  }

  private void assertEventPayload(List<CoreEvent> events) {
    for (CoreEvent event : events) {
      DistributedTraceContextManager distributedTraceContextManager =
          (DistributedTraceContextManager) event.getMessage().getPayload().getValue();
      assertThat(distributedTraceContextManager.getClass().getName(),
                 equalTo("org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager"));
      // This will only resolve the correlation id, as the current span is closed after the flow is executed.
      assertThat(distributedTraceContextManager.getRemoteTraceContextMap(), aMapWithSize(1));
      assertThat(distributedTraceContextManager.getRemoteTraceContextMap(), hasEntry("X-Correlation-ID", "0000-0000"));
    }
  }

  /**
   * An event collector for testing.
   */
  private static class EventCollectorProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (EVENTS) {
        EVENTS.add(event);
      }
      return event;
    }
  }
}
