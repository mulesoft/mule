/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CORRELATION_ID;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CorrelationIdOnSourcesStory.CORRELATION_ID_MODIFICATION;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import reactor.core.publisher.Mono;

@SmallTest
@Feature(CORRELATION_ID)
@Story(CORRELATION_ID_MODIFICATION)
public class ProcessorChildContextChainExecutorTestCase extends AbstractMuleContextTestCase {

  private static final String TEST_CORRELATION_ID = "messirve";

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock(lenient = true)
  private MessageProcessorChain chain;

  private final CorrelationIdProcessor processor = new CorrelationIdProcessor();

  private CoreEvent coreEvent;

  private Latch latch;

  @Before
  public void setUp() throws Exception {
    this.coreEvent = testEvent();
    ComponentLocation someLocation = new DefaultComponentLocation(empty(), emptyList());
    SdkInternalContext content = new SdkInternalContext();
    ((InternalEvent) this.coreEvent).setSdkInternalContext(content);
    content.putContext(someLocation, coreEvent.getCorrelationId());
    when(chain.getLocation()).thenReturn(someLocation);
    when(chain.apply(any())).thenAnswer(inv -> Mono.<CoreEvent>from(inv.getArgument(0))
        .map(event -> {
          try {
            return processor.process(event);
          } catch (MuleException e) {
            return null;
          }
        }));
    when(chain.getMessageProcessors()).thenReturn(singletonList(processor));
  }

  @Test
  public void testDoProcessSuccessOnce() throws InterruptedException {
    ImmutableProcessorChildContextChainExecutor chainExecutor =
        new ImmutableProcessorChildContextChainExecutor(mock(StreamingManager.class), coreEvent, chain);

    AtomicInteger successCalls = new AtomicInteger(0);
    AtomicInteger errorCalls = new AtomicInteger(0);
    Reference<CoreEvent> propagatedEvent = new Reference<>();

    doProcessAndWait(chainExecutor, TEST_CORRELATION_ID, r -> {
      successCalls.incrementAndGet();
      propagatedEvent.set(((EventedResult) r).getEvent());
    }, (t, r) -> errorCalls.incrementAndGet());

    assertThat(successCalls.get(), is(1));
    assertThat(errorCalls.get(), is(0));
    assertThat(processor.correlationID, is(TEST_CORRELATION_ID));
    assertThat(propagatedEvent.get().getCorrelationId(), is(coreEvent.getCorrelationId()));
    assertThat(propagatedEvent.get().getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    // Root ID is the same before, in the middle and after the execution
    assertThat(processor.rootId, is(coreEvent.getContext().getRootId()));
    assertThat(propagatedEvent.get().getContext().getRootId(), is(coreEvent.getContext().getRootId()));
  }

  @Test
  public void testDoProcessOnErrorGenericException() throws InterruptedException {
    ImmutableProcessorChildContextChainExecutor chainExecutor =
        new ImmutableProcessorChildContextChainExecutor(mock(StreamingManager.class), coreEvent, chain);

    Reference<Boolean> parentIsFinished = new Reference<>(false);
    ((BaseEventContext) coreEvent.getContext()).onComplete((ev, t) -> parentIsFinished.set(true));
    processor.throwError();

    AtomicInteger successCalls = new AtomicInteger(0);
    AtomicInteger errorCalls = new AtomicInteger(0);
    Reference<Event> errorEvent = new Reference<>();

    doProcessAndWait(chainExecutor, TEST_CORRELATION_ID, r -> successCalls.incrementAndGet(), (t, r) -> {
      errorCalls.incrementAndGet();
      errorEvent.set(((EventedResult) r).getEvent());
    });

    assertThat(successCalls.get(), is(0));
    assertThat(errorCalls.get(), is(1));
    assertThat(errorEvent.get().getMessage().getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(processor.correlationID, is(TEST_CORRELATION_ID));
    assertThat(errorEvent.get().getCorrelationId(), is(coreEvent.getCorrelationId()));
    assertThat(parentIsFinished.get(), is(false));
  }

  @Test
  public void contextFinished() throws InterruptedException {
    Reference<Boolean> parentFinished = new Reference<>(false);
    Reference<Boolean> newFinished = new Reference<>(false);
    Reference<Boolean> correctCompletionOrder = new Reference<>(false);

    processor.setConsumer((ev, t) -> correctCompletionOrder.set(!newFinished.get()));
    ((BaseEventContext) coreEvent.getContext()).onComplete((ev, t) -> parentFinished.set(true));

    ImmutableProcessorChildContextChainExecutor chainExecutor =
        new ImmutableProcessorChildContextChainExecutor(mock(StreamingManager.class), coreEvent, chain);

    doProcessAndWait(chainExecutor, TEST_CORRELATION_ID, r -> {
      newFinished.set(true);
    }, (t, r) -> {
    });
    // The original context shouldn't be finished (MULE-19772)
    assertThat(parentFinished.get(), is(false));
    // But the created one must be finished
    assertThat(newFinished.get(), is(true));
    assertThat(processor.context.isComplete(), is(true));
    // The created context is called before the onSuccess callback (MULE-19694)
    assertThat(correctCompletionOrder.get(), is(true));
  }


  private void doProcessAndWait(ImmutableProcessorChildContextChainExecutor chainExecutor, String expectedCorrelationId,
                                Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError)
      throws InterruptedException {
    latch = new Latch();
    chainExecutor.process(expectedCorrelationId, onSuccess, onError);
    latch.await(300, MILLISECONDS);
  }

  private static class CorrelationIdProcessor implements Processor {

    public String correlationID = null;
    public String rootId = null;
    public BaseEventContext context = null;
    private BiConsumer<CoreEvent, Throwable> consumer = null;
    private boolean throwError = false;

    public void throwError() {
      this.throwError = true;
    }

    public void setConsumer(BiConsumer<CoreEvent, Throwable> consumer) {
      this.consumer = consumer;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      context = (BaseEventContext) event.getContext();
      correlationID = event.getCorrelationId();
      rootId = event.getContext().getRootId();

      if (consumer != null) {
        context.onComplete(consumer);
      }

      if (throwError) {
        throw new MessagingException(createStaticMessage("some exception"), event);
      }
      return event;
    }
  }

}
