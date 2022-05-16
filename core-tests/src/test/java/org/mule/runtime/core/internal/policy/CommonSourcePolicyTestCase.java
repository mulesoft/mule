package org.mule.runtime.core.internal.policy;

import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.FluxSink;

public class CommonSourcePolicyTestCase extends AbstractMuleTestCase {

  @Test
  public void disposeMustWaitForInflightEventsWithSuccessfulCompletion() {
    List<FluxSink> fluxSinks = new ArrayList<>();
    InternalEvent mockedEvent = mock(InternalEvent.class, RETURNS_MOCKS);
    when(mockedEvent.getSourcePolicyContext())
        .thenReturn((EventInternalContext) (mock(SourcePolicyContext.class, RETURNS_MOCKS)));
    CommonSourcePolicy commonSourcePolicy = getCommonSourcePolicy(fluxSinks);
    commonSourcePolicy.process(mockedEvent, mock(MessageSourceResponseParametersProcessor.class),
                               mock(CompletableCallback.class));
    commonSourcePolicy.dispose();
    fluxSinks.forEach(fluxSink -> verify(fluxSink, never()).complete());
    commonSourcePolicy.finishFlowProcessing(mockedEvent, mock(Either.class));
    fluxSinks.forEach(fluxSink -> verify(fluxSink).complete());
  }

  @Test
  public void disposeMustWaitForInflightEventsWithUnsuccessfulCompletion() {
    List<FluxSink> fluxSinks = new ArrayList<>();
    InternalEvent mockedEvent = mock(InternalEvent.class, RETURNS_MOCKS);
    when(mockedEvent.getSourcePolicyContext())
        .thenReturn((EventInternalContext) (mock(SourcePolicyContext.class, RETURNS_MOCKS)));
    CommonSourcePolicy commonSourcePolicy = getCommonSourcePolicy(fluxSinks);
    commonSourcePolicy.process(mockedEvent, mock(MessageSourceResponseParametersProcessor.class),
                               mock(CompletableCallback.class));
    commonSourcePolicy.dispose();
    fluxSinks.forEach(fluxSink -> verify(fluxSink, never()).complete());
    commonSourcePolicy.finishFlowProcessing(mockedEvent, mock(Either.class), mock(Throwable.class),
                                            mock(SourcePolicyContext.class, RETURNS_MOCKS));
    fluxSinks.forEach(fluxSink -> verify(fluxSink).complete());
  }

  /**
   * Returns a {@link CommonSourcePolicy} instance that will expose the {@link FluxSink} instances it creates.
   * 
   * @param fluxSinks A list where all the {@link FluxSink} instances created by the policy will be added.
   * @return A {@link CommonSourcePolicy}.
   */
  private CommonSourcePolicy getCommonSourcePolicy(List<FluxSink> fluxSinks) {
    CommonSourcePolicy commonSourcePolicy = new CommonSourcePolicy(() -> {
      FluxSink fluxSinkMock = mock(FluxSink.class);
      fluxSinks.add(fluxSinkMock);
      return fluxSinkMock;
    });
    return commonSourcePolicy;
  }

}
