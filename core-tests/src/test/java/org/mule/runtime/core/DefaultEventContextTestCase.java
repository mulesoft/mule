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
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.util.SerializationUtils;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.SerializationTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    parent.success(event);

    assertThat(from(parent.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), equalTo(event));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
  }

  @Test
  @Description("EventContext response publisher completes with null result. Also given response publisher completed and " +
      "there there are no child contexts the completion publisher also completes.")
  public void successNoResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");

    parent.success();

    assertThat(from(parent.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), is(nullValue()));
    assertThat(from(parent.getCompletionPublisher()).blockMillis(BLOCK_TIMEOUT), is(nullValue()));
  }

  @Test
  @Description("EventContext response publisher completes with error. Also given response publisher completed and " +
      "there there are no child contexts the completion publisher also completes.")
  public void error() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");

    RuntimeException exception = new RuntimeException();
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    parent.error(exception);

    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(true));

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

    assertThat(from(child.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), equalTo(event));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(true));
    // Child completion does not complete parent
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));

    parent.success(event);

    assertThat(from(parent.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), equalTo(event));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are " +
      "complete, even when child context completes after parent context response.")
  public void childDelayedSuccessWithResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    Event event = testEvent();
    parent.success(event);

    assertThat(from(parent.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), equalTo(event));
    // Parent context does not complete because it still has uncompleted children
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(false));

    child.success(event);

    assertThat(from(child.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), equalTo(event));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(true));

    // Now child contexts are complete, parent completes
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with no value and all child contexts are " +
      "complete.")
  public void childSuccessWithNoResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    child.success();
    parent.success();

    assertThat(from(child.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), is(nullValue()));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(true));

    assertThat(from(parent.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), is(nullValue()));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with no value and all child contexts are " +
      "complete, even when child context completes after parent context response.")
  public void childDelayedSuccessWithNoResult() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    parent.success();

    assertThat(from(parent.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), is(nullValue()));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(false));

    child.success();

    assertThat(from(child.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), is(nullValue()));

    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
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

    assertThat(from(child.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(true));

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));

  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with error and all child contexts are " +
      "complete, even when child context completes after parent context response.")
  public void childDelayedError() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);

    RuntimeException exception = new RuntimeException();
    parent.error(exception);

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(false));

    child.error(exception);

    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));

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

      assertThat(from(child1.getCompletionPublisher()).toFuture().isDone(), is(false));
      assertThat(from(child1.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), equalTo(event));
      assertThat(from(child1.getCompletionPublisher()).toFuture().isDone(), is(true));

      assertThat(from(parent.getResponsePublisher()).blockMillis(BLOCK_TIMEOUT), equalTo(event));
      assertThat(from(child1.getCompletionPublisher()).toFuture().isDone(), is(true));
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

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(grandchild.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(grandchild.getCompletionPublisher()).toFuture().isDone(), is(false));

    grandchild.success();

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(grandchild.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(grandchild.getCompletionPublisher()).toFuture().isDone(), is(true));

    child.success();

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(grandchild.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(grandchild.getCompletionPublisher()).toFuture().isDone(), is(true));

    parent.success();

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(child.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(grandchild.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(grandchild.getCompletionPublisher()).toFuture().isDone(), is(true));

  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child and grandchild " +
      "contexts are complete, even if parent response is available earlier.")
  public void multipleLevelsParentFirst() throws Exception {
    EventContext parent = create(getTestFlow(muleContext), "");
    EventContext child = DefaultEventContext.child(parent);
    EventContext grandchild = DefaultEventContext.child(child);

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(grandchild.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(grandchild.getCompletionPublisher()).toFuture().isDone(), is(false));

    parent.success();

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(grandchild.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(grandchild.getCompletionPublisher()).toFuture().isDone(), is(false));

    child.success();

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(child.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(grandchild.getResponsePublisher()).toFuture().isDone(), is(false));
    assertThat(from(grandchild.getCompletionPublisher()).toFuture().isDone(), is(false));

    grandchild.success();

    assertThat(from(parent.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(child.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(child.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(grandchild.getResponsePublisher()).toFuture().isDone(), is(true));
    assertThat(from(grandchild.getCompletionPublisher()).toFuture().isDone(), is(true));
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

    assertThat(from(grandchild1.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(grandchild2.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(child1.getCompletionPublisher()).toFuture().isDone(), is(false));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));

    child1.success();
    assertThat(from(child1.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));

    grandchild3.success();
    grandchild4.success();
    child2.success();

    assertThat(from(grandchild3.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(grandchild4.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(child2.getCompletionPublisher()).toFuture().isDone(), is(true));
    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(false));

    parent.success();

    assertThat(from(parent.getCompletionPublisher()).toFuture().isDone(), is(true));
  }

}
