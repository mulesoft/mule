/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer.tracing;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING;
import static org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo.getDefaultChildSpanInfo;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.CoreEventTracer;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;
import org.mockito.Mockito;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunWith(Parameterized.class)
public class TracingErrorPropagationEnablementTestCase extends AbstractMuleContextTestCase {

  private final BiConsumer<CoreEventTracer, CoreEvent> coreEventTracerMethodToExecute;

  public TracingErrorPropagationEnablementTestCase(BiConsumer<CoreEventTracer, CoreEvent> coreEventTracerMethodToExecute) {
    this.coreEventTracerMethodToExecute = coreEventTracerMethodToExecute;
  }

  @Parameterized.Parameters(name = "core event tracer method to test: {0}")
  public static Collection<BiConsumer<CoreEventTracer, CoreEvent>> getParameters() {
    List<BiConsumer<CoreEventTracer, CoreEvent>> consumers = new ArrayList<>();
    consumers.add(CoreEventTracer::endCurrentSpan);
    consumers.add(CoreEventTracer::getDistributedTraceContextMap);
    consumers
        .add(((coreEventTracer, coreEvent) -> coreEventTracer.startComponentSpan(coreEvent, new TestSpanCustomizationInfo())));
    return consumers;
  }

  @Test(expected = TracingErrorException.class)
  @Description("This test shows that the propagation of tracing errors are enabled in tests based on mule contexts")
  public void testEnablementOfDefaultProfilingServiceCoreEventTracerThroughSystemProperty() throws Exception {
    doTestCoreEventTracer();
  }

  @Test
  @Description("This test shows that the propagation of tracing errors are enabled in tests based on mule contexts")
  public void testDisablementOfDefaultProfilingServiceCoreEventTracerThroughSystemProperty() throws Exception {
    System.setProperty(ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING, "false");
    doTestCoreEventTracer();
  }

  private void doTestCoreEventTracer() throws MuleException {
    DefaultProfilingService defaultProfilingService = new DefaultProfilingService();
    muleContext.getInjector().inject(defaultProfilingService);
    CoreEvent coreEvent = Mockito.mock(CoreEvent.class);
    DistributedTraceContextAware eventContext =
        mock(DistributedTraceContextAware.class, withSettings().extraInterfaces(EventContext.class));
    when(coreEvent.getContext()).thenReturn((EventContext) eventContext);
    when(eventContext.getDistributedTraceContext()).thenThrow(new TracingErrorException());
    coreEventTracerMethodToExecute.accept(defaultProfilingService.getCoreEventTracer(), coreEvent);
  }

  /**
   * A {@link TracingErrorException} used for testing the enablement of the error propagation.
   */
  private static class TracingErrorException extends RuntimeException {
  }

  /**
   * A {@link SpanCustomizationInfo} used for testing purposes.
   */
  private static class TestSpanCustomizationInfo implements SpanCustomizationInfo {

    @Override
    public String getName(CoreEvent coreEvent) {
      return "test";
    }

    @Override
    public ChildSpanCustomizationInfo getChildSpanCustomizationInfo() {
      return getDefaultChildSpanInfo();
    }
  }
}
