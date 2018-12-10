/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProcessorChainExecutorTestCase extends AbstractMuleContextTestCase {

  @Mock(lenient = true)
  private MessageProcessorChain chain;

  @Mock
  private Processor processor;

  private CoreEvent coreEvent;

  private Latch latch;

  @Before
  public void setUp() throws Exception {
    this.coreEvent = testEvent();
    when(chain.getLocation()).thenReturn(null);
    when(chain.apply(any())).thenReturn(just(coreEvent));
    when(chain.getMessageProcessors()).thenReturn(singletonList(processor));
  }

  @Test
  public void testDoProcessSuccessOnce() throws InterruptedException {
    ImmutableProcessorChainExecutor chainExecutor = new ImmutableProcessorChainExecutor(coreEvent, chain);


    AtomicInteger successCalls = new AtomicInteger(0);
    AtomicInteger errorCalls = new AtomicInteger(0);

    doProcessAndWait(chainExecutor, r -> successCalls.incrementAndGet(), (t, r) -> errorCalls.incrementAndGet());

    assertThat(successCalls.get(), is(1));
    assertThat(errorCalls.get(), is(0));
  }

  @Test
  public void testDoProcessOnErrorMessagingException() throws InterruptedException, MuleException {
    final String ERROR_PAYLOAD = "ERROR_PAYLOAD";
    when(chain.apply(any())).thenReturn(error(new MessagingException(createStaticMessage(""),
                                                                     getEventBuilder().message(of(ERROR_PAYLOAD)).build())));
    ImmutableProcessorChainExecutor chainExecutor = new ImmutableProcessorChainExecutor(coreEvent, chain);

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
    when(chain.apply(any())).thenReturn(error(new RuntimeException()));
    ImmutableProcessorChainExecutor chainExecutor = new ImmutableProcessorChainExecutor(coreEvent, chain);

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
