/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

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
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.internal.exception.OnErrorPropagateHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;

@SmallTest
public class MessageProcessorsTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private RuntimeException exception = new IllegalArgumentException();
  private EventContext eventContext;
  private Event input;
  private Event output;
  private Event response;
  private Flow flow;

  @Before
  public void setup() throws MuleException {
    flow = mock(Flow.class, RETURNS_DEEP_STUBS);
    OnErrorPropagateHandler exceptionHandler = new OnErrorPropagateHandler();
    exceptionHandler.setMuleContext(muleContext);
    exceptionHandler.initialise();
    when(flow.getExceptionListener()).thenReturn(exceptionHandler);
    eventContext = DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION);
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

  private ReactiveProcessor map = publisher -> from(publisher).map(in -> output);
  private ReactiveProcessor ackAndStop = publisher -> from(publisher).then(in -> {
    in.getInternalContext().success();
    return empty();
  });
  private ReactiveProcessor respondAndStop = publisher -> from(publisher).then(in -> {
    in.getInternalContext().success(response);
    return empty();
  });
  private ReactiveProcessor ackAndMap =
      publisher -> from(publisher).doOnNext(in -> in.getInternalContext().success()).map(in -> output);
  private ReactiveProcessor respondAndMap =
      publisher -> from(publisher).doOnNext(in -> in.getInternalContext().success(response)).map(in -> output);
  private ReactiveProcessor error = publisher -> from(publisher).map(in -> {
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
    thrown.expect((is(instanceOf(MessagingException.class))));
    thrown.expectCause(is(exception));
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

  private Processor createChain(ReactiveProcessor processor) throws InitialisationException {
    MessageProcessorChain chain = newChain(new ReactiveProcessorToProcessorAdaptor(processor));
    chain.setMuleContext(muleContext);
    return chain;
  }

  private Processor createFlow(ReactiveProcessor processor) throws MuleException {
    flow = Flow.builder("test", muleContext).processors(new ReactiveProcessorToProcessorAdaptor(processor)).build();
    flow.initialise();
    flow.start();
    return flow;
  }

  private static class ReactiveProcessorToProcessorAdaptor implements Processor {

    ReactiveProcessor delegate;

    ReactiveProcessorToProcessorAdaptor(ReactiveProcessor delegate) {
      this.delegate = delegate;
    }

    @Override
    public Event process(Event event) throws MuleException {
      throw new UnsupportedOperationException();
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
      return delegate.apply(publisher);
    }
  }

}
