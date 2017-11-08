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
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.reactivestreams.Publisher;

public class ExecutableComponentTestCase extends AbstractMuleContextTestCase {

  private TestExecutableComponent executableComponent = new TestExecutableComponent();
  private Message requestMessage;
  private Message responseMessage = of("Response");
  private AtomicReference<CoreEvent> componentInEvent = new AtomicReference<>();


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
  public void testExecuteWithEvent() throws Exception {
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


  final class TestExecutableComponent extends AbstractExecutableComponent implements ReactiveProcessor {

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher).map(event -> {
        componentInEvent.set(event);
        return builder(event).message(responseMessage).build();
      });
    }
  }


}
