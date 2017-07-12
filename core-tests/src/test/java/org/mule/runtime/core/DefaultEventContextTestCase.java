/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.time.Duration.ofMillis;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.RESPONSE_AND_COMPLETION_PUBLISHERS;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import reactor.core.publisher.MonoProcessor;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(EVENT_CONTEXT)
@Stories(RESPONSE_AND_COMPLETION_PUBLISHERS)
public class DefaultEventContextTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  @Description("EventContext response publisher completes with value of result. Also given response publisher completed and " +
      "there there are no child contexts the completion publisher also completes.")
  public void successWithResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);

    Event event = testEvent();
    assertCompletionNotDone(parent);
    parent.success(event);

    awaitAndAssertResponse(parent, event);
    assertCompletionDone(parent);
  }

  @Test
  @Description("EventContext response publisher completes with null result. Also given response publisher completed and " +
      "there there are no child contexts the completion publisher also completes.")
  public void successNoResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);

    parent.success();

    awaittNullResponse(parent);
    assertBeforeResponseDone(parent);
    assertThat(from(parent.getCompletionPublisher()).block(ofMillis(BLOCK_TIMEOUT)), is(nullValue()));
  }

  @Test
  @Description("EventContext response publisher completes with error. Also given response publisher completed and " +
      "there there are no child contexts the completion publisher also completes.")
  public void error() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);

    RuntimeException exception = new RuntimeException();
    assertCompletionNotDone(parent);
    parent.error(exception);

    assertCompletionDone(parent);

    assertResponseDone(parent);
    assertBeforeResponseDone(parent);

    expectedException.expect(is(exception));
    from(parent.getResponsePublisher()).block(ofMillis(BLOCK_TIMEOUT));
  }

  @Test
  @Description("EventContext beforeResponsePublisher subscribers are notified before responsePublisher subscribers (assuming " +
      "both are subscribed before response is completed)")
  public void beforeResponse() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);

    Event event = testEvent();

    Latch latch = new Latch();
    Latch lock = new Latch();
    Latch responseSubscriberFired = new Latch();
    from(parent.getBeforeResponsePublisher()).doOnNext(checkedConsumer(e -> {
      lock.countDown();
      latch.await();
    })).subscribe();
    from(parent.getResponsePublisher()).doOnNext(checkedConsumer(e -> {
      responseSubscriberFired.countDown();
    })).subscribe();

    Scheduler testScheduler = muleContext.getSchedulerService().ioScheduler();

    try {
      testScheduler.submit(() -> parent.success(event));

      // Wait until 'before response' publisher subscriber is fired
      lock.await();

      // Assert that `response` publisher subscriber is not fired until `before response` subscriber finishes
      assertThat(responseSubscriberFired.await(BLOCK_TIMEOUT, MILLISECONDS), is(false));

      // Unblock `before response` publisher
      latch.countDown();

      // Assert that `response` publisher subscriber is now fired
      assertThat(responseSubscriberFired.await(BLOCK_TIMEOUT, MILLISECONDS), is(true));

      awaitAndAssertResponse(parent, event);
      awaitCompletion(parent);
    } finally {
      testScheduler.stop();
    }
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete.")
  public void childSuccessWithResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child = DefaultEventContext.child(parent, empty());

    Event event = testEvent();

    child.success(event);

    awaitAndAssertResponse(child, event);
    assertCompletionDone(child);
    // Child completion does not complete parent
    assertCompletionNotDone(parent);

    parent.success(event);

    awaitAndAssertResponse(parent, event);
    assertCompletionDone(parent);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete, even when child context completes after parent context response.")
  public void childDelayedSuccessWithResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child = DefaultEventContext.child(parent, empty());

    Event event = testEvent();
    parent.success(event);

    awaitAndAssertResponse(parent, event);
    // Parent context does not complete because it still has uncompleted children
    assertCompletionNotDone(parent);
    assertCompletionNotDone(child);

    child.success(event);

    awaitAndAssertResponse(child, event);
    assertCompletionDone(child);

    // Now child contexts are complete, parent completes
    assertCompletionDone(parent);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with no value and all child contexts are " +
      "complete.")
  public void childSuccessWithNoResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child = DefaultEventContext.child(parent, empty());

    child.success();
    parent.success();

    awaittNullResponse(child);
    assertCompletionDone(child);

    awaittNullResponse(parent);
    assertCompletionDone(parent);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with no value and all child contexts are " +
      "complete, even when child context completes after parent context response.")
  public void childDelayedSuccessWithNoResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child = DefaultEventContext.child(parent, empty());

    parent.success();

    awaittNullResponse(parent);
    assertCompletionNotDone(parent);
    assertCompletionNotDone(child);

    child.success();

    awaittNullResponse(child);

    assertCompletionDone(child);
    assertCompletionDone(parent);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with error and all child contexts are " +
      "complete.")
  public void childError() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child = DefaultEventContext.child(parent, empty());

    RuntimeException exception = new RuntimeException();
    child.error(exception);
    parent.error(exception);

    assertResponseDone(child);
    assertCompletionDone(child);

    assertResponseDone(parent);
    assertCompletionDone(parent);

  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with error and all child contexts are " +
      "complete, even when child context completes after parent context response.")
  public void childDelayedError() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child = DefaultEventContext.child(parent, empty());

    RuntimeException exception = new RuntimeException();
    parent.error(exception);

    assertResponseDone(parent);
    assertCompletionNotDone(parent);
    assertCompletionNotDone(child);

    child.error(exception);

    assertCompletionDone(parent);
    assertCompletionDone(parent);

    expectedException.expect(is(exception));
    from(child.getResponsePublisher()).block(ofMillis(BLOCK_TIMEOUT));
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete, even when child is run async with a delay.")
  public void asyncChild() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child1 = DefaultEventContext.child(parent, empty());

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
      awaitAndAssertResponse(child1, event);
      awaitCompletion(child1);

      awaitAndAssertResponse(parent, event);
      assertCompletionDone(child1);
    } finally {
      testScheduler.stop();
    }
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child and grandchild " +
      "contexts are complete.")
  public void multipleLevelsGrandchildFirst() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child = DefaultEventContext.child(parent, empty());
    EventContext grandchild = DefaultEventContext.child(child, empty());

    assertResponseNotDone(parent);
    assertCompletionNotDone(parent);
    assertResponseNotDone(child);
    assertCompletionNotDone(child);
    assertResponseNotDone(grandchild);
    assertCompletionNotDone(grandchild);

    grandchild.success();

    assertResponseNotDone(parent);
    assertCompletionNotDone(parent);
    assertResponseNotDone(child);
    assertCompletionNotDone(child);
    assertResponseDone(grandchild);
    assertCompletionDone(grandchild);

    child.success();

    assertResponseNotDone(parent);
    assertCompletionNotDone(parent);
    assertResponseDone(child);
    assertCompletionDone(child);
    assertResponseDone(grandchild);
    assertCompletionDone(grandchild);

    parent.success();

    assertResponseDone(parent);
    assertCompletionDone(parent);
    assertResponseDone(child);
    assertCompletionDone(child);
    assertResponseDone(grandchild);
    assertCompletionDone(grandchild);

  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child and grandchild " +
      "contexts are complete, even if parent response is available earlier.")
  public void multipleLevelsParentFirst() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child = DefaultEventContext.child(parent, empty());
    EventContext grandchild = DefaultEventContext.child(child, empty());

    assertResponseNotDone(parent);
    assertCompletionNotDone(parent);
    assertResponseNotDone(child);
    assertCompletionNotDone(child);
    assertResponseNotDone(grandchild);
    assertCompletionNotDone(grandchild);

    parent.success();

    assertResponseDone(parent);
    assertCompletionNotDone(parent);
    assertResponseNotDone(child);
    assertCompletionNotDone(child);
    assertResponseNotDone(grandchild);
    assertCompletionNotDone(grandchild);

    child.success();

    assertResponseDone(parent);
    assertCompletionNotDone(parent);
    assertResponseDone(child);
    assertCompletionNotDone(child);
    assertResponseNotDone(grandchild);
    assertCompletionNotDone(grandchild);

    grandchild.success();

    assertResponseDone(parent);
    assertCompletionDone(parent);
    assertResponseDone(child);
    assertCompletionDone(child);
    assertResponseDone(grandchild);
    assertCompletionDone(grandchild);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete, even if one branch of the tree completes.")
  public void multipleBranches() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);
    EventContext child1 = DefaultEventContext.child(parent, empty());
    EventContext child2 = DefaultEventContext.child(parent, empty());

    EventContext grandchild1 = DefaultEventContext.child(child1, empty());
    EventContext grandchild2 = DefaultEventContext.child(child1, empty());
    EventContext grandchild3 = DefaultEventContext.child(child2, empty());
    EventContext grandchild4 = DefaultEventContext.child(child2, empty());

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
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION, null, externalCompletion);

    Event event = testEvent();
    assertCompletionNotDone(parent);
    parent.success(event);

    awaitAndAssertResponse(parent, event);
    assertCompletionNotDone(parent);

    externalCompletion.onComplete();
    assertCompletionDone(parent);
  }

  @Test
  @Description("EventContext response publisher completes with error but the completion publisher only completes "
      + " once the external publisher completes.")
  public void externalCompletionError() throws Exception {
    MonoProcessor<Void> externalCompletion = MonoProcessor.create();
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION, null, externalCompletion);

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
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION, null, externalCompletion);
    EventContext child = DefaultEventContext.child(parent, empty());

    Event event = testEvent();

    child.success(event);

    awaitAndAssertResponse(child, event);
    assertCompletionDone(child);
    // Child completion does not complete parent
    assertCompletionNotDone(parent);

    parent.success(event);

    awaitAndAssertResponse(parent, event);
    assertCompletionNotDone(parent);

    externalCompletion.onComplete();
    assertCompletionDone(parent);
  }

  @Test
  @Description("When a child event context is de-serialized it is decoupled from parent context but response and completion " +
      "publisher still complete when a response event is available.")
  public void deserializedChild() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION, null);
    EventContext child = DefaultEventContext.child(parent, empty());

    byte[] bytes = muleContext.getObjectSerializer().getExternalProtocol().serialize(child);
    EventContext deserializedChild = muleContext.getObjectSerializer().getExternalProtocol().deserialize(bytes);

    Event event = testEvent();

    deserializedChild.success(event);

    awaitAndAssertResponse(deserializedChild, event);
    assertCompletionDone(deserializedChild);
  }

  @Test
  @Description("When a parent event context is de-serialized the parent context no longer waits for completion of child context.")
  public void deserializedParent() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION, null);
    EventContext child = DefaultEventContext.child(parent, empty());

    byte[] bytes = muleContext.getObjectSerializer().getExternalProtocol().serialize(parent);
    EventContext deserializedParent = muleContext.getObjectSerializer().getExternalProtocol().deserialize(bytes);

    Event event = testEvent();

    deserializedParent.success(event);

    awaitAndAssertResponse(deserializedParent, event);
    assertCompletionDone(deserializedParent);
  }

  @Test
  @Description("Verify that a location produces connector and source data.")
  public void componentData() throws Exception {
    TypedComponentIdentifier typedComponentIdentifier = TypedComponentIdentifier.builder()
        .withType(SOURCE)
        .withIdentifier(buildFromStringRepresentation("http:listener"))
        .build();
    ComponentLocation location = mock(ComponentLocation.class);
    when(location.getComponentIdentifier()).thenReturn(typedComponentIdentifier);
    EventContext context = create(getTestFlow(muleContext), location);

    assertThat(context.getOriginatingConnectorName(), is("http"));
    assertThat(context.getOriginatingSourceName(), is("listener"));
  }

  @Test
  @Description("Verify that a single component location produces connector and source data.")
  public void componentDataFromSingleComponent() throws Exception {
    EventContext context = create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION);

    assertThat(context.getOriginatingConnectorName(), is(CORE_PREFIX));
    assertThat(context.getOriginatingSourceName(), is("test"));
  }

  private void assertBeforeResponseDone(EventContext parent) {
    assertThat(from(parent.getBeforeResponsePublisher()).toFuture().isDone(), is(true));
  }

  private void awaitAndAssertResponse(EventContext parent, Event event) {
    assertThat(from(parent.getResponsePublisher()).block(ofMillis(BLOCK_TIMEOUT)), equalTo(event));
  }

  private void awaittNullResponse(EventContext child) {
    assertThat(from(child.getResponsePublisher()).block(ofMillis(BLOCK_TIMEOUT)), is(nullValue()));
  }

  private void assertResponseDone(EventContext parent) {
    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(true));
  }

  private void assertResponseNotDone(EventContext parent) {
    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(false));
  }

  private void awaitCompletion(EventContext parent) {
    assertThat(from(parent.getCompletionPublisher()).block(ofMillis(BLOCK_TIMEOUT)), is(nullValue()));
  }

  private void assertCompletionDone(EventContext parent) {
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isCompletedExceptionally(), is(false));
  }

  private void assertCompletionNotDone(EventContext child1) {
    assertThat(from(child1.getCompletionPublisher()).toFuture().isDone(), is(false));
  }

}
