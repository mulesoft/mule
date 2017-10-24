/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.ThrowableCauseMatcher.hasCause;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.runtime.core.privileged.processor.MessageProcessors;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;

import java.util.Optional;

@SmallTest
public class MessageProcessorsTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private RuntimeException exception = new IllegalArgumentException();
  private BaseEventContext eventContext;
  private CoreEvent input;
  private CoreEvent output;
  private CoreEvent response;
  private Flow flow;

  @Before
  public void setup() throws MuleException {
    flow = mock(Flow.class, RETURNS_DEEP_STUBS);
    OnErrorPropagateHandler exceptionHandler = new OnErrorPropagateHandler();
    exceptionHandler.setMuleContext(muleContext);
    exceptionHandler
        .setNotificationFirer(((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(NotificationDispatcher.class));
    exceptionHandler.initialise();
    when(flow.getExceptionListener()).thenReturn(exceptionHandler);
    eventContext = (BaseEventContext) create(flow, TEST_CONNECTOR_LOCATION);
    input = builder(eventContext).message(of(TEST_MESSAGE)).build();
    output = builder(eventContext).message(of(TEST_MESSAGE)).build();
    response = builder(eventContext).message(of(TEST_MESSAGE)).build();
  }

  @After
  public void tearDown() throws MuleException {
    if (flow != null) {
      flow.stop();
      flow.dispose();
    }
  }

  private InternalReactiveProcessor map = publisher -> from(publisher).map(in -> output);
  private InternalReactiveProcessor ackAndStop = publisher -> from(publisher).flatMap(in -> {
    ((BaseEventContext) in.getContext()).success();
    return empty();
  });
  private InternalReactiveProcessor respondAndStop = publisher -> from(publisher).flatMap(in -> {
    ((BaseEventContext) in.getContext()).success(response);
    return empty();
  });
  private InternalReactiveProcessor ackAndMap =
      publisher -> from(publisher).doOnNext(in -> ((BaseEventContext) in.getContext()).success()).map(in -> output);
  private InternalReactiveProcessor respondAndMap =
      publisher -> from(publisher).doOnNext(in -> ((BaseEventContext) in.getContext()).success(response)).map(in -> output);
  private InternalReactiveProcessor error = publisher -> from(publisher).map(in -> {
    throw exception;
  });

  @Test
  public void processToApplyMap() throws Exception {
    assertThat(processToApply(input, map), is(output));
    assertThat(from(eventContext.getResponsePublisher()).toFuture().isDone(), is(false));
  }

  @Test
  public void processToApplyMapInChain() throws Exception {
    assertThat(processToApply(input, createChain(map)), is(output));
    assertThat(from(eventContext.getResponsePublisher()).toFuture().isDone(), is(false));
  }

  @Test
  public void processToApplyMapInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(map)).getMessage(), is(output.getMessage()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(output));
  }

  @Test
  public void processToApplyAckAndStop() throws Exception {
    assertThat(processToApply(input, ackAndStop), is(nullValue()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processToApplyAckAndStopInChain() throws Exception {
    assertThat(processToApply(input, createChain(ackAndStop)), is(nullValue()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processToApplyAckAndStopInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(ackAndStop)), is(nullValue()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processToApplyRespondAndStop() throws Exception {
    assertThat(processToApply(input, respondAndStop), is(response));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processToApplyRespondAndStopInChain() throws Exception {
    assertThat(processToApply(input, createChain(respondAndStop)), is(response));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processToApplyRespondAndStopInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(respondAndStop)).getMessage(), is(response.getMessage()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processToApplyAckAndMap() throws Exception {
    assertThat(processToApply(input, ackAndMap), is(output));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processToApplyAckAndMapInChain() throws Exception {
    assertThat(processToApply(input, createChain(ackAndMap)), is(output));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processToApplyAckAndMapInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(ackAndMap)), is(nullValue()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processToApplyRespondAndMap() throws Exception {
    assertThat(processToApply(input, respondAndMap), is(output));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processToApplyRespondAndMapInChain() throws Exception {
    assertThat(processToApply(input, createChain(respondAndMap)), is(output));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processToApplyRespondAndMapInFlow() throws Exception {
    assertThat(processToApply(input, createFlow(respondAndMap)).getMessage(), is(response.getMessage()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processToApplyError() throws Exception {
    thrown.expect(is(exception));
    try {
      processToApply(input, error);
    } finally {
      assertThat(from(eventContext.getResponsePublisher()).toFuture().isDone(), is(false));
    }
  }

  @Test
  public void processToApplyErrorInChain() throws Exception {
    try {
      processToApply(input, createChain(error));
      fail("Exception expected");
    } catch (Throwable t) {
      assertThat(t, is(instanceOf(MessagingException.class)));
      assertThat(t.getCause(), is(exception));
    }

    assertThat(from(eventContext.getResponsePublisher()).toFuture().isDone(), is(true));

    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    from(eventContext.getResponsePublisher()).block();
  }

  @Test
  public void processToApplyErrorInFlow() throws Exception {

    try {
      processToApply(input, createFlow(error));
      fail("Exception expected");
    } catch (Throwable t) {
      assertThat(t, is(instanceOf(MessagingException.class)));
      assertThat(t.getCause(), is(exception));
    }

    assertThat(from(eventContext.getResponsePublisher()).toFuture().isDone(), is(true));

    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    from(eventContext.getResponsePublisher()).block();
  }

  @Test
  public void processMap() throws Exception {
    assertThat(from(MessageProcessors.process(input, map)).block(), is(output));
    assertThat(from(eventContext.getResponsePublisher()).toFuture().get(), equalTo(output));
  }

  @Test
  public void processMapInChain() throws Exception {
    assertThat(from(MessageProcessors.process(input, createChain(map))).block(), is(output));
    assertThat(from(eventContext.getResponsePublisher()).toFuture().get(), equalTo(output));
  }

  @Test
  public void processMapInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(map))).block().getMessage(), is(output.getMessage()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(output));
  }

  @Test
  public void processAckAndStop() throws Exception {
    assertThat(from(MessageProcessors.process(input, ackAndStop)).block(), is(nullValue()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processAckAndStopInChain() throws Exception {
    assertThat(from(MessageProcessors.process(input, createChain(ackAndStop))).block(), is(nullValue()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processAckAndStopInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(ackAndStop))).block(), is(nullValue()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processRespondAndStop() throws Exception {
    assertThat(from(MessageProcessors.process(input, respondAndStop)).block(), is(response));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processRespondAndStopInChain() throws Exception {
    assertThat(from(MessageProcessors.process(input, createChain(respondAndStop))).block(), is(response));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processRespondAndStopInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(respondAndStop))).block().getMessage(),
               is(response.getMessage()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processAckAndMap() throws Exception {
    assertThat(from(MessageProcessors.process(input, ackAndMap)).block(), is(output));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processAckAndMapInChain() throws Exception {
    assertThat(from(MessageProcessors.process(input, createChain(ackAndMap))).block(), is(output));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processAckAndMapInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(ackAndMap))).block(), is(nullValue()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(nullValue()));
  }

  @Test
  public void processRespondAndMap() throws Exception {
    assertThat(from(MessageProcessors.process(input, respondAndMap)).block(), is(output));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processRespondAndMapInChain() throws Exception {
    assertThat(from(MessageProcessors.process(input, createChain(respondAndMap))).block(), is(output));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processRespondAndMapInFlow() throws Exception {
    assertThat(from(MessageProcessors.process(input, createFlow(respondAndMap))).block().getMessage(), is(response.getMessage()));
    assertThat(from(eventContext.getResponsePublisher()).block(), is(response));
  }

  @Test
  public void processError() throws Exception {
    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    try {
      from(MessageProcessors.process(input, error)).block();
    } finally {
      assertThat(from(eventContext.getResponsePublisher()).toFuture().isDone(), is(true));
      from(eventContext.getResponsePublisher()).toFuture()
          .whenComplete((event, throwable) -> assertThat(throwable.getCause(), equalTo(exception)));
    }
  }

  @Test
  public void processErrorInChain() throws Exception {
    try {
      from(MessageProcessors.process(input, createChain(error))).block();
      fail("Exception expected");
    } catch (Throwable t) {
      assertThat(t.getCause(), is(instanceOf(MessagingException.class)));
      assertThat(t.getCause().getCause(), is(exception));
    }

    assertThat(from(eventContext.getResponsePublisher()).toFuture().isDone(), is(true));

    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    from(eventContext.getResponsePublisher()).block();
  }

  @Test
  public void processErrorInFlow() throws Exception {

    try {
      from(MessageProcessors.process(input, createFlow(error))).block();
      fail("Exception expected");
    } catch (Throwable t) {
      assertThat(t.getCause(), is(instanceOf(MessagingException.class)));
      assertThat(t.getCause().getCause(), is(exception));
    }

    assertThat(from(eventContext.getResponsePublisher()).toFuture().isDone(), is(true));

    thrown.expectCause((is(instanceOf(MessagingException.class))));
    thrown.expectCause(hasCause(is(exception)));
    from(eventContext.getResponsePublisher()).block();
  }


  private Processor createChain(ReactiveProcessor processor) throws InitialisationException {
    MessageProcessorChain chain = newChain(Optional.empty(), new ReactiveProcessorToProcessorAdaptor(processor));
    chain.setMuleContext(muleContext);
    return chain;
  }

  private Processor createFlow(ReactiveProcessor processor) throws MuleException {
    flow = Flow.builder("test", muleContext).processors(new ReactiveProcessorToProcessorAdaptor(processor)).build();
    flow.initialise();
    flow.start();
    return flow;
  }

  private static class ReactiveProcessorToProcessorAdaptor implements Processor, InternalProcessor {

    ReactiveProcessor delegate;

    ReactiveProcessorToProcessorAdaptor(ReactiveProcessor delegate) {
      this.delegate = delegate;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return delegate.apply(publisher);
    }
  }

  @FunctionalInterface
  private interface InternalReactiveProcessor extends ReactiveProcessor, InternalProcessor {

  }

}
