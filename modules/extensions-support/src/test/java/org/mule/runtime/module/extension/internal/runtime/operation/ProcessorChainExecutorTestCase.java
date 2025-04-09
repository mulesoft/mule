/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;
import static reactor.core.publisher.Mono.error;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.event.InternalEvent;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.core.privileged.event.EventedResult;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import reactor.core.publisher.Mono;

public class ProcessorChainExecutorTestCase extends AbstractMuleContextTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock(lenient = true)
  private MessageProcessorChain chain;

  @Mock
  private Processor processor;

  private CoreEvent coreEvent;

  private Latch latch;

  @Before
  public void setUp() throws Exception {
    this.coreEvent = testEvent();
    ((InternalEvent) this.coreEvent).setSdkInternalContext(new SdkInternalContext());
    when(chain.getLocation()).thenReturn(null);
    when(chain.apply(any())).thenAnswer(inv -> Mono.<CoreEvent>from(inv.getArgument(0))
        .map(event -> CoreEvent.builder(event)
            .message(coreEvent.getMessage())
            .variables(coreEvent.getVariables())
            .build()));
    when(chain.getMessageProcessors()).thenReturn(singletonList(processor));
  }

  @Test
  public void testDoProcessSuccessOnce() throws InterruptedException {
    ImmutableProcessorChainExecutor chainExecutor =
        new ImmutableProcessorChainExecutor(mock(StreamingManager.class), coreEvent, chain);

    AtomicInteger successCalls = new AtomicInteger(0);
    AtomicInteger errorCalls = new AtomicInteger(0);

    doProcessAndWait(chainExecutor, r -> successCalls.incrementAndGet(), (t, r) -> errorCalls.incrementAndGet());

    assertThat(successCalls.get(), is(1));
    assertThat(errorCalls.get(), is(0));
  }

  @Test
  public void testDoProcessOnErrorMessagingException() throws InterruptedException, MuleException {
    final String ERROR_PAYLOAD = "ERROR_PAYLOAD";
    doReturn(error(new MessagingException(createStaticMessage(""),
                                          getEventBuilder().message(of(ERROR_PAYLOAD)).build())))
        .when(chain).apply(any());
    ImmutableProcessorChainExecutor chainExecutor =
        new ImmutableProcessorChainExecutor(mock(StreamingManager.class), coreEvent, chain);

    AtomicInteger successCalls = new AtomicInteger(0);
    AtomicInteger errorCalls = new AtomicInteger(0);
    Reference<Event> errorEvent = new Reference<>();

    doProcessAndWait(chainExecutor,
                     r -> successCalls.incrementAndGet(),
                     (t, r) -> {
                       errorCalls.incrementAndGet();
                       errorEvent.set(((EventedResult) r).getEvent());
                     });

    assertThat(successCalls.get(), is(0));
    assertThat(errorCalls.get(), is(1));
    assertThat(errorEvent.get().getMessage().getPayload().getValue(), is(ERROR_PAYLOAD));
  }

  @Test
  public void testDoProcessOnErrorGenericException() throws InterruptedException {
    doReturn(error(new RuntimeException())).when(chain).apply(any());
    ImmutableProcessorChainExecutor chainExecutor =
        new ImmutableProcessorChainExecutor(mock(StreamingManager.class), coreEvent, chain);

    AtomicInteger successCalls = new AtomicInteger(0);
    AtomicInteger errorCalls = new AtomicInteger(0);
    Reference<Event> errorEvent = new Reference<>();

    doProcessAndWait(chainExecutor,
                     r -> successCalls.incrementAndGet(),
                     (t, r) -> {
                       errorCalls.incrementAndGet();
                       errorEvent.set(((EventedResult) r).getEvent());
                     });

    assertThat(successCalls.get(), is(0));
    assertThat(errorCalls.get(), is(1));
    assertThat(errorEvent.get().getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
  }

  private void doProcessAndWait(ImmutableProcessorChainExecutor chainExecutor,
                                Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError)
      throws InterruptedException {
    latch = new Latch();
    chainExecutor.process(onSuccess, onError);
    latch.await(300, MILLISECONDS);
  }

}
