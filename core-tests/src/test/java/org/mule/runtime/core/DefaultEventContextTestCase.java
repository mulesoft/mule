/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.core.publisher.MonoProcessor;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("EventContext")
@Stories("EventContext response and completion publishers")
public class DefaultEventContextTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  @Description("EventContext response publisher completes with value of result. Also given response publisher completed and " +
      "there there are no child contexts the completion publisher also completes.")
  public void successWithResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");

    Event event = testEvent();
    assertCompletionNotDone(parent);
    parent.success(event);

    assertResponse(parent, event);
    assertCompletionDone(parent);
  }

  @Test
  @Description("EventContext response publisher completes with null result. Also given response publisher completed and " +
      "there there are no child contexts the completion publisher also completes.")
  public void successNoResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");

    parent.success();

    assertNullResponse(parent);
    assertThat(from(parent.getCompletionPublisher()).blockMillis(BLOCK_TIMEOUT), is(nullValue()));
  }

  @Test
  @Description("EventContext response publisher completes with error. Also given response publisher completed and " +
      "there there are no child contexts the completion publisher also completes.")
  public void error() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");

    RuntimeException exception = new RuntimeException();
    assertCompletionNotDone(parent);
    parent.error(exception);

    assertCompletionDone(parent);

    assertResponseNotDone(parent);

    expectedException.expect(is(exception));
    from(parent.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete.")
  public void childSuccessWithResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    Event event = testEvent();

    child.success(event);

    assertResponse(child, event);
    assertCompletionDone(child);
    // Child completion does not complete parent
    assertCompletionNotDone(parent);

    parent.success(event);

    assertResponse(parent, event);
    assertCompletionDone(parent);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete, even when child context completes after parent context response.")
  public void childDelayedSuccessWithResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    Event event = testEvent();
    parent.success(event);

    assertResponse(parent, event);
    // Parent context does not complete because it still has uncompleted children
    assertCompletionNotDone(parent);
    assertCompletionNotDone(child);

    child.success(event);

    assertResponse(child, event);
    assertCompletionDone(child);

    // Now child contexts are complete, parent completes
    assertCompletionDone(parent);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with no value and all child contexts are " +
      "complete.")
  public void childSuccessWithNoResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    child.success();
    parent.success();

    assertNullResponse(child);
    assertCompletionDone(child);

    assertNullResponse(parent);
    assertCompletionDone(parent);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with no value and all child contexts are " +
      "complete, even when child context completes after parent context response.")
  public void childDelayedSuccessWithNoResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    parent.success();

    assertNullResponse(parent);
    assertCompletionNotDone(parent);
    assertCompletionNotDone(child);

    child.success();

    assertNullResponse(child);

    assertCompletionDone(child);
    assertCompletionDone(parent);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with error and all child contexts are " +
      "complete.")
  public void childError() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    RuntimeException exception = new RuntimeException();
    child.error(exception);
    parent.error(exception);

    assertResponseNotDone(child);
    assertCompletionDone(child);

    assertResponseNotDone(parent);
    assertCompletionDone(parent);

  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with error and all child contexts are " +
      "complete, even when child context completes after parent context response.")
  public void childDelayedError() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    RuntimeException exception = new RuntimeException();
    parent.error(exception);

    assertResponseNotDone(parent);
    assertCompletionNotDone(parent);
    assertCompletionNotDone(child);

    child.error(exception);

    assertCompletionDone(parent);
    assertCompletionDone(parent);

    expectedException.expect(is(exception));
    from(child.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete, even when child is run async with a delay.")
  public void asyncChild() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child1 = DefaultEventContext.child(parent);

    Event event = testEvent();
    Scheduler testScheduler = muleContext.getSchedulerService().ioScheduler();

    try {
      testScheduler.submit(() -> {
        Thread.sleep(5);
        child1.success(event);
        return null;
      });

      parent.success(event);

      assertCompletionNotDone(child1);
      assertResponse(child1, event);
      assertCompletionDone(child1);

      assertResponse(parent, event);
      assertCompletionDone(child1);
    } finally {
      testScheduler.shutdown();
    }
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child and grandchild " +
      "contexts are complete.")
  public void multipleLevelsGrandchildFirst() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);
    EventContext grandchild = DefaultEventContext.child(child);

    assertResponseDone(parent);
    assertCompletionNotDone(parent);
    assertResponseDone(child);
    assertCompletionNotDone(child);
    assertResponseDone(grandchild);
    assertCompletionNotDone(grandchild);

    grandchild.success();

    assertResponseDone(parent);
    assertCompletionNotDone(parent);
    assertResponseDone(child);
    assertCompletionNotDone(child);
    assertResponseNotDone(grandchild);
    assertCompletionDone(grandchild);

    child.success();

    assertResponseDone(parent);
    assertCompletionNotDone(parent);
    assertResponseNotDone(child);
    assertCompletionDone(child);
    assertResponseNotDone(grandchild);
    assertCompletionDone(grandchild);

    parent.success();

    assertResponseNotDone(parent);
    assertCompletionDone(parent);
    assertResponseNotDone(child);
    assertCompletionDone(child);
    assertResponseNotDone(grandchild);
    assertCompletionDone(grandchild);

  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child and grandchild " +
      "contexts are complete, even if parent response is available earlier.")
  public void multipleLevelsParentFirst() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);
    EventContext grandchild = DefaultEventContext.child(child);

    assertResponseDone(parent);
    assertCompletionNotDone(parent);
    assertResponseDone(child);
    assertCompletionNotDone(child);
    assertResponseDone(grandchild);
    assertCompletionNotDone(grandchild);

    parent.success();

    assertResponseNotDone(parent);
    assertCompletionNotDone(parent);
    assertResponseDone(child);
    assertCompletionNotDone(child);
    assertResponseDone(grandchild);
    assertCompletionNotDone(grandchild);

    child.success();

    assertResponseNotDone(parent);
    assertCompletionNotDone(parent);
    assertResponseNotDone(child);
    assertCompletionNotDone(child);
    assertResponseDone(grandchild);
    assertCompletionNotDone(grandchild);

    grandchild.success();

    assertResponseNotDone(parent);
    assertCompletionDone(parent);
    assertResponseNotDone(child);
    assertCompletionDone(child);
    assertResponseNotDone(grandchild);
    assertCompletionDone(grandchild);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete, even if one branch of the tree completes.")
  public void multipleBranches() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child1 = DefaultEventContext.child(parent);
    EventContext child2 = DefaultEventContext.child(parent);

    EventContext grandchild1 = DefaultEventContext.child(child1);
    EventContext grandchild2 = DefaultEventContext.child(child1);
    EventContext grandchild3 = DefaultEventContext.child(child2);
    EventContext grandchild4 = DefaultEventContext.child(child2);

    grandchild1.success();
    grandchild2.success();

    assertCompletionDone(grandchild1);
    assertCompletionDone(grandchild2);
    assertCompletionNotDone(child1);
    assertCompletionNotDone(parent);

    child1.success();
    assertCompletionDone(child1);
    assertCompletionNotDone(parent);

    grandchild3.success();
    grandchild4.success();
    child2.success();

    assertCompletionDone(grandchild3);
    assertCompletionDone(grandchild4);
    assertCompletionDone(child2);
    assertCompletionNotDone(parent);

    parent.success();

    assertCompletionDone(parent);
  }

  @Test
  @Description("EventContext response publisher completes with value of result but the completion publisher only completes "
      + " once the external publisher completes.")
  public void externalCompletionSuccess() throws Exception {
    MonoProcessor<Void> externalCompletion = MonoProcessor.create();
    EventContext parent = create(getTestFlow(muleContext), "", null, externalCompletion);

    Event event = testEvent();
    assertCompletionNotDone(parent);
    parent.success(event);

    assertResponse(parent, event);
    assertCompletionNotDone(parent);

    externalCompletion.onComplete();
    assertCompletionDone(parent);
  }

  @Test
  @Description("EventContext response publisher completes with error but the completion publisher only completes "
      + " once the external publisher completes.")
  public void externalCompletionError() throws Exception {
    MonoProcessor<Void> externalCompletion = MonoProcessor.create();
    EventContext parent = create(getTestFlow(muleContext), "", null, externalCompletion);

    RuntimeException exception = new RuntimeException();
    assertCompletionNotDone(parent);
    parent.error(exception);

    assertCompletionNotDone(parent);

    externalCompletion.onComplete();
    assertCompletionDone(parent);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete and external completion completes.")
  public void externalCompletionWithChild() throws Exception {
    MonoProcessor<Void> externalCompletion = MonoProcessor.create();
    EventContext parent = create(getTestFlow(muleContext), "", null, externalCompletion);
    EventContext child = DefaultEventContext.child(parent);

    Event event = testEvent();

    child.success(event);

    assertResponse(child, event);
    assertCompletionDone(child);
    // Child completion does not complete parent
    assertCompletionNotDone(parent);

    parent.success(event);

    assertResponse(parent, event);
    assertCompletionNotDone(parent);

    externalCompletion.onComplete();
    assertCompletionDone(parent);
  }

  @Test
  @Description("When a child event context is serialized the parent context no longer waits for completion of child context.")
  public void childSerializationUnregistersWithParent() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "", null);
    EventContext child = DefaultEventContext.child(parent);

    muleContext.getObjectSerializer().getExternalProtocol().serialize(child);

    Event event = testEvent();

    parent.success(event);

    assertResponse(parent, event);
    assertCompletionDone(parent);
  }

  @Test
  @Description("When a child event context is de-serialized it completion works in the same way.")
  public void deserializedChild() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "", null);
    EventContext child = DefaultEventContext.child(parent);

    byte[] bytes = muleContext.getObjectSerializer().getExternalProtocol().serialize(child);
    EventContext deserializedChild = muleContext.getObjectSerializer().getExternalProtocol().deserialize(bytes);

    Event event = testEvent();

    deserializedChild.success(event);

    assertResponse(deserializedChild, event);
    assertCompletionDone(deserializedChild);
  }

  @Test
  @Description("When a parent event context is de-serialized the parent context no longer waits for completion of child context.")
  public void deserializedParent() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "", null);
    EventContext child = DefaultEventContext.child(parent);

    byte[] bytes = muleContext.getObjectSerializer().getExternalProtocol().serialize(parent);
    EventContext deserializedParent = muleContext.getObjectSerializer().getExternalProtocol().deserialize(bytes);

    Event event = testEvent();

    deserializedParent.success(event);

    assertResponse(deserializedParent, event);
    assertCompletionDone(deserializedParent);
  }


  private void assertResponse(EventContext parent, Event event) {
    assertThat(from(parent.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), equalTo(event));
  }

  private void assertNullResponse(EventContext child) {
    assertThat(from(child.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), is(nullValue()));
  }

  private void assertResponseDone(EventContext parent) {
    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(false));
  }

  private void assertResponseNotDone(EventContext parent) {
    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(true));
  }

  private void assertCompletionDone(EventContext parent) {
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
  }

  private void assertCompletionNotDone(EventContext child1) {
    assertThat(from(child1.getCompletionPublisher()).toFuture().isDone(), is(false));
  }

}
