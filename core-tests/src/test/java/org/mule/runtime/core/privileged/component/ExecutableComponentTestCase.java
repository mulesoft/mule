/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.component;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromFuture;

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
  public void testExecuteWithInputEvent() throws Exception {
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

    parentContext.success();
    assertThat(parentContext.isTerminated(), is(true));
  }

  @Test
  public void executeWithCoreEvent() throws Exception {
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

    parentContext.success();
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

  private Event mockEvent() throws MuleException {
    final Event event = mock(Event.class);

    when(event.getMessage()).thenReturn(testEvent().getMessage());
    when(event.getContext()).thenReturn(testEvent().getContext());
    when(event.getError()).thenReturn(empty());

    return event;
  }

  final class TestExecutableComponent extends AbstractExecutableComponent implements ReactiveProcessor {

    private boolean hang;

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      if (hang) {
        return fromFuture(new CompletableFuture<>());
      } else {
        return from(publisher).map(event -> {
          componentInEvent.set(event);
          return builder(event).message(responseMessage).build();
        });
      }
    }

    public void setHang(boolean hang) {
      this.hang = hang;
    }
  }


}
