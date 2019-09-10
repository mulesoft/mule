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
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
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

      assertThat(eventContext.isTerminated(), is(false));
    }
  }

  @Test
  public void executeWithEventError() throws Exception {
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
  public void executeWithContributor() throws MuleException {
    executableComponent.execute(testEvent(), eb -> eb.addVariable("its_me", "Mario!"));
    assertThat(componentInEvent.get().getVariables().get("its_me").getValue(), is("Mario!"));
  }

  final class TestExecutableComponent extends AbstractExecutableComponent implements ReactiveProcessor {

    private Throwable toThrow;

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
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

    public void setToThrow(Throwable toThrow) {
      this.toThrow = toThrow;
    }
  }

}
