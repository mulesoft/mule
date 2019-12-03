/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.tck.MuleTestUtils.OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

@RunWith(MockitoJUnitRunner.class)
public class NestedProcessorValueResolverTestCase extends AbstractMuleContextTestCase {

  @Mock(lenient = true)
  private MessageProcessorChain messageProcessor;

  @Mock
  private ExpressionManager expressionManager;

  @Before
  public void before() throws Exception {
    final CoreEvent testEvent = testEvent();
    when(messageProcessor.process(any(CoreEvent.class))).thenReturn(testEvent);
    when(messageProcessor.apply(any(Publisher.class))).thenReturn(Mono.just(testEvent));
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(OBJECT_ERROR_TYPE_REPO_REGISTRY_KEY, createDefaultErrorTypeRepository());
  }

  @Test
  public void yieldsNestedProcessor() throws Exception {
    ProcessorChainValueResolver resolver = new ProcessorChainValueResolver(muleContext, messageProcessor);
    final CoreEvent event = testEvent();

    Chain nestedProcessor = resolver.resolve(ValueResolvingContext.builder(event)
        .withExpressionManager(expressionManager)
        .build());
    nestedProcessor.process(result -> {
      assertThat(result.getOutput(), is(TEST_PAYLOAD));

      ArgumentCaptor<CoreEvent> captor = ArgumentCaptor.forClass(CoreEvent.class);
      try {
        verify(messageProcessor).process(captor.capture());
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }

      CoreEvent capturedEvent = captor.getValue();
      assertThat(capturedEvent, is(event));
    }, (e, r) -> fail(e.getMessage()));
  }

  @Test
  public void alwaysGivesDifferentInstances() throws Exception {
    ProcessorChainValueResolver resolver = new ProcessorChainValueResolver(muleContext, messageProcessor);
    ValueResolvingContext ctx = ValueResolvingContext.builder(testEvent()).withExpressionManager(expressionManager).build();
    Chain resolved1 = resolver.resolve(ctx);
    Chain resolved2 = resolver.resolve(ctx);

    assertThat(resolved1, is(not(sameInstance(resolved2))));
  }

  @Test
  public void chainIsCalledAsNonBlocking() throws Exception {
    ProcessorChainValueResolver resolver = new ProcessorChainValueResolver(muleContext, messageProcessor);

    Chain resolve = resolver.resolve(ValueResolvingContext.builder(testEvent())
        .withExpressionManager(expressionManager)
        .build());

    resolve.process(result -> {
    }, (t, r) -> {
    });

    verify(messageProcessor, times(1)).apply(any());
    verify(messageProcessor, times(0)).process(any());
  }

}
