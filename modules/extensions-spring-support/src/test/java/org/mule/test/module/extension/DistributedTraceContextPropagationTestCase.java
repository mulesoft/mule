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

import org.hamcrest.collection.IsMapWithSize;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.streaming.HasSize;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class DistributedTraceContextPropagationTestCase extends AbstractExtensionFunctionalTestCase {

  public static final String TRACE_CONTEXT_PROPAGATION = "traceContextPropagation";

  private static final long PROBER_TIMEOUT = 15000;
  private static final long PROBER_FREQUENCY = 1000;

  @Override
  protected String getConfigFile() {
    return "distributed-trace-context-propagation-test.xml";
  }

  @Test
  public void defaultTraceContextPropagator() throws Exception {
    startFlow();
    check(PROBER_TIMEOUT, PROBER_FREQUENCY, () -> EventCollectorProcessor.getEvents().size() == 1);

    for (CoreEvent event : EventCollectorProcessor.getEvents()) {
      DistributedTraceContextManager distributedTraceContextManager =
          (DistributedTraceContextManager) event.getMessage().getPayload().getValue();
      assertThat(distributedTraceContextManager.getClass().getName(),
                 equalTo("org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager"));
      assertThat(distributedTraceContextManager.getRemoteTraceContextMap(), aMapWithSize(2));
      assertThat(distributedTraceContextManager.getRemoteTraceContextMap(), hasEntry("X-Correlation-ID", "0000-0000"));
    }
  }

  private void startFlow() throws Exception {
    ((Startable) getFlowConstruct(TRACE_CONTEXT_PROPAGATION)).start();
  }

  /**
   * An event collector for testing.
   */
  private static class EventCollectorProcessor extends AbstractComponent implements Processor {

    private final static List<CoreEvent> EVENTS = new LinkedList<>();

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      EVENTS.add(event);
      return event;
    }

    public static List<CoreEvent> getEvents() {
      return EVENTS;
    }
  }
}
