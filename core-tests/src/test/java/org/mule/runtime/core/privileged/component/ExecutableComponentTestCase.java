/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.component;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromFuture;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.reactivestreams.Publisher;

public class ExecutableComponentTestCase extends AbstractMuleContextTestCase {

  private final TestExecutableComponent executableComponent = new TestExecutableComponent();
  private Message requestMessage;
  private final Message responseMessage = of("Response");
  private final AtomicReference<CoreEvent> componentInEvent = new AtomicReference<>();


  @Override
  protected void doSetUp() throws Exception {
    muleContext.getInjector().inject(executableComponent);
    requestMessage = testEvent().getMessage();
  }

  @Test
  public void executeWithInputEvent() throws Exception {
    ExecutionResult executionResult = executableComponent.execute(InputEvent.create().message(requestMessage)).get();
    Event response = executionResult.getEvent();

    assertThat(componentInEvent.get().getMessage(), equalTo(requestMessage));
    assertThat(response.getMessage(), equalTo(responseMessage));

    assertThat(componentInEvent.get().getContext(), equalTo(response.getContext()));

    BaseEventContext eventContext = (BaseEventContext) componentInEvent.get().getContext();
    assertThat(eventContext.isTerminated(), is(false));

    executionResult.complete();
    assertThat(eventContext.isTerminated(), is(true));
  }

  @Test
  public void executeWithEvent() throws Exception {
    Event response = executableComponent.execute(mockEvent()).get();

    assertThat(componentInEvent.get().getMessage(), equalTo(requestMessage));
    assertThat(response.getMessage(), equalTo(responseMessage));

    assertThat(componentInEvent.get().getContext(), not(equalTo(response.getContext())));
    assertThat(((BaseEventContext) componentInEvent.get().getContext()).isTerminated(), is(true));
    assertThat(((BaseEventContext) response.getContext()).isTerminated(), is(false));

    BaseEventContext childContext = (BaseEventContext) componentInEvent.get().getContext();
    assertThat(childContext.isTerminated(), is(true));

    BaseEventContext parentContext = (BaseEventContext) testEvent().getContext();
    assertThat(parentContext.isTerminated(), is(false));

    ((BaseEventContext) testEvent().getContext()).success();
    assertThat(parentContext.isTerminated(), is(true));
  }

  @Test
  public void executeWithInputEventError() throws Exception {
    executableComponent.setToThrow(new IllegalStateException("Expected"));

    try {
      executableComponent.execute(InputEvent.create().message(requestMessage)).get();
      fail("ComponentExecutionException expected");
    } catch (java.util.concurrent.ExecutionException ee) {
      ComponentExecutionException cee = (ComponentExecutionException) ee.getCause();
      final Event errorEvent = cee.getEvent();

      assertThat(componentInEvent.get().getMessage(), equalTo(requestMessage));
      assertThat(errorEvent.getMessage(), equalTo(testEvent().getMessage()));

      assertThat(componentInEvent.get().getContext(), equalTo(errorEvent.getContext()));

      BaseEventContext eventContext = (BaseEventContext) componentInEvent.get().getContext();

      assertThat(eventContext.isTerminated(), is(true));
    }
  }

  @Test
  public void executeWithEventError() throws Exception {
    executableComponent.setToThrow(new IllegalStateException("Expected"));

    try {
      executableComponent.execute(mockEvent()).get();
      fail("ComponentExecutionException expected");
    } catch (java.util.concurrent.ExecutionException ee) {
      ComponentExecutionException cee = (ComponentExecutionException) ee.getCause();
      final Event errorEvent = cee.getEvent();

      assertThat(componentInEvent.get().getContext(), equalTo(errorEvent.getContext()));
      assertThat(errorEvent.getMessage(), equalTo(testEvent().getMessage()));

      assertThat(((BaseEventContext) componentInEvent.get().getContext()).isTerminated(), is(true));

      BaseEventContext childContext = (BaseEventContext) componentInEvent.get().getContext();
      assertThat(childContext.isTerminated(), is(true));

      BaseEventContext parentContext = (BaseEventContext) testEvent().getContext();
      assertThat(parentContext.isTerminated(), is(false));

      ((BaseEventContext) testEvent().getContext()).success();
      assertThat(parentContext.isTerminated(), is(true));
    }
  }

  @Test
  public void executeWithContributor() throws MuleException {
    executableComponent.execute(mockEvent(), eb -> eb.addVariable("its_me", "Mario!"));
    assertThat(componentInEvent.get().getVariables().get("its_me").getValue(), is("Mario!"));
  }

  @Test
  public void executeWithCoreEvent() throws Exception {
    Event response = executableComponent.execute(testEvent()).get();

    assertThat(componentInEvent.get().getMessage(), equalTo(requestMessage));
    assertThat(response.getMessage(), equalTo(responseMessage));

    assertThat(componentInEvent.get().getContext(), not(equalTo(response.getContext())));
    assertThat(((BaseEventContext) componentInEvent.get().getContext()).isTerminated(), is(true));
    assertThat(((BaseEventContext) response.getContext()).isTerminated(), is(false));

    BaseEventContext childContext = (BaseEventContext) componentInEvent.get().getContext();
    assertThat(childContext.isTerminated(), is(true));

    BaseEventContext parentContext = (BaseEventContext) testEvent().getContext();
    assertThat(parentContext.isTerminated(), is(false));

    ((BaseEventContext) testEvent().getContext()).success();
    assertThat(parentContext.isTerminated(), is(true));
  }

  @Test
  public void executeWithCoreEventCancel() throws Exception {
    executableComponent.setHang(true);
    final Event event = testEvent();
    final CompletableFuture<Event> executeResult = executableComponent.execute(event);
    executeResult.cancel(true);

    ((BaseEventContext) event.getContext()).success();

    assertThat("Most probaly a child context has not terminated yet.", ((BaseEventContext) event.getContext()).isTerminated(),
               is(true));
  }

  @Test
  public void executeWithCoreEventError() throws Exception {
    executableComponent.setToThrow(new IllegalStateException("Expected"));

    try {
      executableComponent.execute(testEvent()).get();
      fail("ComponentExecutionException expected");
    } catch (java.util.concurrent.ExecutionException ee) {
      ComponentExecutionException cee = (ComponentExecutionException) ee.getCause();
      final Event errorEvent = cee.getEvent();

      assertThat(componentInEvent.get().getContext(), not(equalTo(errorEvent.getContext())));
      assertThat(errorEvent.getMessage(), equalTo(testEvent().getMessage()));

      assertThat(((BaseEventContext) componentInEvent.get().getContext()).isTerminated(), is(true));
      assertThat(((BaseEventContext) errorEvent.getContext()).isTerminated(), is(false));

      BaseEventContext childContext = (BaseEventContext) componentInEvent.get().getContext();
      assertThat(childContext.isTerminated(), is(true));

      BaseEventContext parentContext = (BaseEventContext) testEvent().getContext();
      assertThat(parentContext.isTerminated(), is(false));

      ((BaseEventContext) testEvent().getContext()).success();
      assertThat(parentContext.isTerminated(), is(true));
    }
  }

  @Test
  public void executeWithCoreEventContributor() throws MuleException {
    executableComponent.execute(testEvent(), eb -> eb.addVariable("its_me", "Mario!"));
    assertThat(componentInEvent.get().getVariables().get("its_me").getValue(), is("Mario!"));
  }

  @Test
  public void executeWithCoreEventContributorCancel() throws MuleException {
    executableComponent.setHang(true);
    final Event event = testEvent();
    final CompletableFuture<Event> executeResult = executableComponent.execute(event, eb -> eb.addVariable("its_me", "Mario!"));
    executeResult.cancel(true);

    ((BaseEventContext) event.getContext()).success();

    assertThat("Most probaly a child context has not terminated yet.", ((BaseEventContext) event.getContext()).isTerminated(),
               is(true));
  }

  private Event mockEvent() throws MuleException {
    final Event event = mock(Event.class);

    when(event.getMessage()).thenReturn(testEvent().getMessage());
    when(event.getContext()).thenReturn(testEvent().getContext());

    return event;
  }

  final class TestExecutableComponent extends AbstractExecutableComponent implements ReactiveProcessor {

    private Throwable toThrow;
    private boolean hang;

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      if (hang) {
        return fromFuture(new CompletableFuture<>());
      } else {
        return from(publisher)
            .doOnNext(event -> componentInEvent.set(event))
            .flatMap(event -> {
              if (toThrow != null) {
                return error(toThrow);
              } else {
                return just(builder(event).message(responseMessage).build());
              }
            });
      }
    }

    public void setToThrow(Throwable toThrow) {
      this.toThrow = toThrow;
    }

    public void setHang(boolean hang) {
      this.hang = hang;
    }
  }

}
