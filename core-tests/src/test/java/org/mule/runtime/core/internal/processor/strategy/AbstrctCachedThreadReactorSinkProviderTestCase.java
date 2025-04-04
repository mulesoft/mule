/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.core.api.event.CoreEvent;
import reactor.core.publisher.FluxSink;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Coverage tests for the {@link AbstractCachedThreadReactorSinkProvider} Note that the name of the test class is mis-spelled
 * intentionally as test cases that start with 'Abstract' are not run.
 *
 */
@ExtendWith(MockitoExtension.class)
class AbstrctCachedThreadReactorSinkProviderTestCase {

  private AbstractCachedThreadReactorSinkProvider provider;
  @Mock
  private CoreEvent event;
  @Mock
  private FluxSink<CoreEvent> sink;

  @Test
  void getSink_indexEnabled() {
    provider = mock(AbstractCachedThreadReactorSinkProvider.class,
                    withSettings().useConstructor(true).defaultAnswer(CALLS_REAL_METHODS));
    // Mock the abstract method so we get something back...
    when(provider.createSink()).thenReturn(sink);

    final FluxSink<CoreEvent> result = provider.getSink();
    result.next(event);
    result.complete();

    verify(sink).next(event);
    verify(sink).complete();
  }

  @Test
  void getSink_indexDisabled() {
    provider = mock(AbstractCachedThreadReactorSinkProvider.class,
                    withSettings().useConstructor(false).defaultAnswer(CALLS_REAL_METHODS));
    // Mock the abstract method so we get something back...
    when(provider.createSink()).thenReturn(sink);

    final FluxSink<CoreEvent> result = provider.getSink();
    result.next(event);
    result.complete();

    verify(sink).next(event);
    verify(sink).complete();
  }

  @Test
  void disposeCompletesOpenSinks() {
    provider = mock(AbstractCachedThreadReactorSinkProvider.class,
                    withSettings().useConstructor(false).defaultAnswer(CALLS_REAL_METHODS));
    when(provider.createSink()).thenReturn(sink);

    final FluxSink<CoreEvent> result = provider.getSink();
    result.next(event);

    provider.dispose();

    verify(sink).complete();
  }

}
