/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.empty;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.FluxSink;

public class CommonSourcePolicyTestCase extends AbstractMuleTestCase {

  private CommonSourcePolicy commonSourcePolicy;
  private List<FluxSink<CoreEvent>> fluxSinks;
  private InternalEvent mockedEvent;

  @Before
  public void setup() {
    fluxSinks = new ArrayList<>();
    commonSourcePolicy = getCommonSourcePolicy(fluxSinks);
    BaseEventContext baseEventContext =
        new DefaultEventContext(mock(FlowConstruct.class), mock(ComponentLocation.class), "", empty());
    mockedEvent = mock(InternalEvent.class, RETURNS_MOCKS);
    when(mockedEvent.getContext()).thenReturn(baseEventContext);
    when(mockedEvent.getSourcePolicyContext())
        .thenReturn((EventInternalContext) (mock(SourcePolicyContext.class, RETURNS_MOCKS)));
  }

  @Test
  public void drainMustWaitForInflightEventsWithSuccessfulResponse() {
    setup();
    commonSourcePolicy.process(mockedEvent, mock(MessageSourceResponseParametersProcessor.class),
                               mock(CompletableCallback.class));
    commonSourcePolicy.drain(CommonSourcePolicy::dispose);
    fluxSinks.forEach(fluxSink -> verify(fluxSink, never()).complete());
    mockedEvent.getContext().success(mockedEvent);
    fluxSinks.forEach(fluxSink -> verify(fluxSink).complete());
  }

  @Test
  public void drainMustWaitForInflightEventsWithEmptyResponse() {
    setup();
    commonSourcePolicy.process(mockedEvent, mock(MessageSourceResponseParametersProcessor.class),
                               mock(CompletableCallback.class));
    commonSourcePolicy.drain(CommonSourcePolicy::dispose);
    fluxSinks.forEach(fluxSink -> verify(fluxSink, never()).complete());
    mockedEvent.getContext().success();
    fluxSinks.forEach(fluxSink -> verify(fluxSink).complete());
  }

  @Test
  public void disposeMustWaitForInflightEventsWithUnsuccessfulCompletion() {
    setup();
    commonSourcePolicy.process(mockedEvent, mock(MessageSourceResponseParametersProcessor.class),
                               mock(CompletableCallback.class));
    commonSourcePolicy.drain(CommonSourcePolicy::dispose);
    fluxSinks.forEach(fluxSink -> verify(fluxSink, never()).complete());
    mockedEvent.getContext()
        .error(new RuntimeException("When the seagulls follow the trawler, it's because they think sardines will be thrown into the sea."));
    fluxSinks.forEach(fluxSink -> verify(fluxSink).complete());
  }

  /**
   * Returns a {@link CommonSourcePolicy} instance that will expose the {@link FluxSink} instances it creates.
   * 
   * @param fluxSinks A list where all the {@link FluxSink} instances created by the policy will be added.
   * @return A {@link CommonSourcePolicy}.
   */
  private CommonSourcePolicy getCommonSourcePolicy(List<FluxSink<CoreEvent>> fluxSinks) {
    FluxSinkSupplier<CoreEvent> sinkSupplier = new FluxSinkSupplier<CoreEvent>() {

      @Override
      public FluxSink<CoreEvent> get() {
        FluxSink<CoreEvent> fluxSinkMock = mock(FluxSink.class);
        fluxSinks.add(fluxSinkMock);
        return fluxSinkMock;
      }

      @Override
      public void dispose() {
        // Nothing to do
      }
    };
    return new CommonSourcePolicy(sinkSupplier);
  }

}
