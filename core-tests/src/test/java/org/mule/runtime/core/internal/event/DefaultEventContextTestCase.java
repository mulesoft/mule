/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.RESPONSE_AND_COMPLETION_PUBLISHERS;

import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.DefaultLocationPart;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * TODO MULE-14000 Create hamcrest matchers to assert EventContext state
 */
@Feature(EVENT_CONTEXT)
@Story(RESPONSE_AND_COMPLETION_PUBLISHERS)
@RunWith(Parameterized.class)
public class DefaultEventContextTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Supplier<DefaultEventContext> context;
  private Function<CompletableFuture<Void>, BaseEventContext> contextWithCompletion;
  private Function<ComponentLocation, BaseEventContext> contextWithComponentLocation;

  private BaseEventContext parent;
  private BaseEventContext child;

  private AtomicReference<CoreEvent> parentResultValue = new AtomicReference<>();
  private AtomicReference<Throwable> parentErrorValue = new AtomicReference<>();
  private AtomicBoolean parentCompletion = new AtomicBoolean();
  private AtomicBoolean parentTerminated = new AtomicBoolean();

  private AtomicReference<CoreEvent> childResultValue = new AtomicReference<>();
  private AtomicReference<Throwable> childErrorValue = new AtomicReference<>();
  private AtomicBoolean childCompletion = new AtomicBoolean();


  public DefaultEventContextTestCase(Supplier<DefaultEventContext> context,
                                     Function<CompletableFuture<Void>, BaseEventContext> contextWithCompletion,
                                     Function<ComponentLocation, BaseEventContext> contextWithComponentLocation) {
    this.context = context;
    this.contextWithCompletion = contextWithCompletion;
    this.contextWithComponentLocation = contextWithComponentLocation;
  }

  @Before
  public void setup() {
    this.parent = context.get();
    setupParentListeners(parent);
  }

  private BaseEventContext addChild(BaseEventContext parent) {
    this.child = child(parent, empty());
    setupChildListeners(child);
    return child;
  }

  private void setupParentListeners(BaseEventContext parent) {
    parent.onResponse((event, throwable) -> {
      parentResultValue.set(event);
      parentErrorValue.set(throwable);
    });
    parent.onComplete((response, throwable) -> parentCompletion.set(true));
    parent.onTerminated((response, throwable) -> parentTerminated.set(true));
  }

  private void setupChildListeners(BaseEventContext child) {
    child.onResponse((event, throwable) -> {
      childResultValue.set(event);
      childErrorValue.set(throwable);
    });
    child.onTerminated((response, throwable) -> childCompletion.set(true));
  }

  @Parameters
  public static List<Object[]> data() {
    return asList(new Object[][] {
        {
            (CheckedSupplier<EventContext>) () -> create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION),
            (CheckedFunction<CompletableFuture<Void>, EventContext>) externalCompletion -> create(getTestFlow(muleContext),
                                                                                                  TEST_CONNECTOR_LOCATION,
                                                                                                  null,
                                                                                                  of(externalCompletion)),
            (CheckedFunction<ComponentLocation, EventContext>) location -> create(getTestFlow(muleContext), location)
        },
        {
            (CheckedSupplier<EventContext>) () -> create("id", DefaultEventContextTestCase.class.getName(),
                                                         TEST_CONNECTOR_LOCATION, NullExceptionHandler.getInstance()),
            (CheckedFunction<CompletableFuture<Void>, EventContext>) externalCompletion -> create("id",
                                                                                                  DefaultEventContextTestCase.class
                                                                                                      .getName(),
                                                                                                  TEST_CONNECTOR_LOCATION,
                                                                                                  null,
                                                                                                  of(externalCompletion),
                                                                                                  NullExceptionHandler
                                                                                                      .getInstance()),
            (CheckedFunction<ComponentLocation, EventContext>) location -> create("id",
                                                                                  DefaultEventContextTestCase.class
                                                                                      .getName(),
                                                                                  location,
                                                                                  NullExceptionHandler.getInstance())
        }
    });
  }

  @Test
  @Description("EventContext response publisher completes with value of result. Also given response publisher completed and there there are no child contexts the completion publisher also completes.")
  public void successWithResult() throws Exception {
    CoreEvent event = testEvent();
    parent.success(event);

    assertParent(is(event), is(nullValue()), true, true);
  }

  @Test
  @Description("EventContext response publisher completes with null result. Also given response publisher completed and there there are no child contexts the completion publisher also completes.")
  public void successNoResult() throws Exception {
    parent.success();

    assertParent(nullValue(), is(nullValue()), true, true);
  }

  @Test
  @Description("EventContext response publisher completes with error. Also given response publisher completed and there there are no child contexts the completion publisher also completes.")
  public void error() throws Exception {
    Exception exception = new Exception();
    parent.error(exception);

    assertParent(nullValue(), is(exception), true, true);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are complete.")
  public void childSuccessWithResult() throws Exception {
    child = addChild(parent);

    CoreEvent event = testEvent();
    child.success(event);

    assertChild(is(event), is(nullValue()), true);

    assertParent(is(nullValue()), is(nullValue()), false, false);

    parent.success(event);

    assertParent(is(event), is(nullValue()), true, true);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are complete, even when child context completes after parent context response.")
  public void childDelayedSuccessWithResult() throws Exception {
    child = addChild(parent);

    CoreEvent event = testEvent();
    parent.success(event);

    assertParent(is(event), is(nullValue()), false, false);

    child.success(event);

    assertChild(is(event), is(nullValue()), true);
    assertParent(is(event), is(nullValue()), true, true);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with no value and all child contexts are complete.")
  public void childSuccessWithNoResult() throws Exception {
    addChild(parent);

    child.success();
    parent.success();

    assertChild(is(nullValue()), is(nullValue()), true);
    assertParent(is(nullValue()), is(nullValue()), true, true);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with no value and all child contexts are complete, even when child context completes after parent context response.")
  public void childDelayedSuccessWithNoResult() throws Exception {
    child = addChild(parent);

    parent.success();

    assertParent(is(nullValue()), is(nullValue()), false, false);

    child.success();

    assertChild(is(nullValue()), is(nullValue()), true);
    assertParent(is(nullValue()), is(nullValue()), true, true);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with error and all child contexts are complete.")
  public void childError() throws Exception {
    child = addChild(parent);

    RuntimeException exception = new RuntimeException();
    child.error(exception);
    parent.error(exception);

    assertParent(is(nullValue()), is(exception), true, true);
    assertChild(is(nullValue()), is(exception), true);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with error and all child contexts are complete, even when child context completes after parent context response.")
  public void childDelayedError() throws Exception {
    child = addChild(parent);

    RuntimeException exception = new RuntimeException();
    parent.error(exception);

    assertParent(is(nullValue()), is(exception), false, false);

    child.error(exception);

    assertParent(is(nullValue()), is(exception), true, true);
    assertChild(is(nullValue()), is(exception), true);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are complete, even when child is run async with a delay.")
  public void asyncChild() throws Exception {
    child = addChild(parent);

    CoreEvent event = testEvent();
    Scheduler testScheduler = muleContext.getSchedulerService().ioScheduler();

    Latch latch1 = new Latch();

    try {
      testScheduler.submit(() -> {
        child.success(event);
        latch1.countDown();
        return null;
      });

      assertParent(is(nullValue()), is(nullValue()), false, false);

      parent.success(event);
      latch1.await();

      assertChild(is(event), is(nullValue()), true);
      assertParent(is(event), is(nullValue()), true, true);
    } finally {
      testScheduler.stop();
    }
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child and grandchild contexts are complete.")
  public void multipleLevelsGrandchildFirst() throws Exception {
    child = addChild(parent);
    BaseEventContext grandchild = child(child, empty());

    grandchild.success(testEvent());

    assertChild(is(nullValue()), is(nullValue()), false);
    assertParent(is(nullValue()), is(nullValue()), false, false);

    child.success(testEvent());

    assertChild(is(testEvent()), is(nullValue()), true);
    assertParent(is(nullValue()), is(nullValue()), false, false);

    parent.success(testEvent());

    assertChild(is(testEvent()), is(nullValue()), true);
    assertParent(is(testEvent()), is(nullValue()), true, true);

  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child and grandchild contexts are complete, even if parent response is available earlier.")
  public void multipleLevelsParentFirst()
      throws Exception {
    child = addChild(parent);
    BaseEventContext grandchild = child(child, empty());

    parent.success(testEvent());

    assertChild(is(nullValue()), is(nullValue()), false);
    assertParent(is(testEvent()), is(nullValue()), false, false);

    child.success(testEvent());

    assertChild(is(testEvent()), is(nullValue()), false);
    assertParent(is(testEvent()), is(nullValue()), false, false);

    grandchild.success();

    assertChild(is(testEvent()), is(nullValue()), true);
    assertParent(is(testEvent()), is(nullValue()), true, true);
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are complete, even if one branch of the tree completes.")
  public void multipleBranches() throws Exception {
    BaseEventContext parent = context.get();
    BaseEventContext child1 = child(parent, empty());
    BaseEventContext child2 = child(parent, empty());

    BaseEventContext grandchild1 = child(child1, empty());
    BaseEventContext grandchild2 = child(child1, empty());
    BaseEventContext grandchild3 = child(child2, empty());
    BaseEventContext grandchild4 = child(child2, empty());

    grandchild1.success();
    grandchild2.success();

    assertThat(grandchild1.isTerminated(), is(true));
    assertThat(grandchild2.isTerminated(), is(true));
    assertThat(child1.isTerminated(), is(false));
    assertThat(parent.isTerminated(), is(false));

    child1.success();
    assertThat(child1.isTerminated(), is(true));
    assertThat(parent.isTerminated(), is(false));

    grandchild3.success();
    grandchild4.success();
    child2.success();

    assertThat(grandchild3.isTerminated(), is(true));
    assertThat(grandchild4.isTerminated(), is(true));
    assertThat(child2.isTerminated(), is(true));
    assertThat(parent.isTerminated(), is(false));

    parent.success();

    assertThat(parent.isTerminated(), is(true));
  }

  @Test
  @Description("EventContext response publisher completes with value of result but the completion publisher only completes once the external publisher completes.")
  public void externalCompletionSuccess() throws Exception {
    CompletableFuture<Void> externalCompletion = new CompletableFuture<>();
    parent = contextWithCompletion.apply(externalCompletion);
    setupParentListeners(parent);

    CoreEvent event = testEvent();
    assertThat(parent.isTerminated(), is(false));
    parent.success(event);

    assertParent(is(event), is(nullValue()), true, false);

    externalCompletion.complete(null);
    assertThat(parent.isTerminated(), is(true));
  }

  @Test
  @Description("EventContext response publisher completes with error but the completion publisher only completes once the external publisher completes.")
  public void externalCompletionError() throws Exception {
    CompletableFuture<Void> externalCompletion = new CompletableFuture<>();
    parent = contextWithCompletion.apply(externalCompletion);
    setupParentListeners(parent);

    RuntimeException exception = new RuntimeException();
    assertThat(parent.isTerminated(), is(false));
    parent.error(exception);

    assertParent(is(nullValue()), is(exception), true, false);

    externalCompletion.complete(null);
    assertThat(parent.isTerminated(), is(true));
  }

  @Test
  @Description("Parent EventContext only completes once response publisher completes with a value and all child contexts are complete and external completion completes.")
  public void externalCompletionWithChild() throws Exception {
    CompletableFuture<Void> externalCompletion = new CompletableFuture<>();
    parent = contextWithCompletion.apply(externalCompletion);
    setupParentListeners(parent);
    child = addChild(parent);

    CoreEvent event = testEvent();

    child.success(event);

    assertChild(is(event), is(nullValue()), true);
    assertThat(parent.isTerminated(), is(false));

    parent.success(event);

    assertParent(is(event), is(nullValue()), true, false);

    externalCompletion.complete(null);
    assertThat(parent.isTerminated(), is(true));
  }

  @Test
  @Description("When a child event context is de-serialized it is decoupled from parent context but response and completion publisher still complete when a response event is available.")
  public void deserializedChild() throws Exception {
    child = addChild(parent);

    byte[] bytes = muleContext.getObjectSerializer().getExternalProtocol().serialize(child);
    child = muleContext.getObjectSerializer().getExternalProtocol().deserialize(bytes);
    setupChildListeners(child);

    child.success(testEvent());

    assertChild(is(testEvent()), is(nullValue()), true);
  }

  @Test
  @Description("When a parent event context is de-serialized the parent context no longer waits for completion of childcontext.")
  public void deserializedParent()
      throws Exception {
    child = addChild(parent);

    byte[] bytes = muleContext.getObjectSerializer().getExternalProtocol().serialize(parent);
    parent = muleContext.getObjectSerializer().getExternalProtocol().deserialize(bytes);
    setupParentListeners(parent);

    parent.success(testEvent());

    assertParent(is(testEvent()), is(nullValue()), true, true);
  }

  @Test
  @Description("Verify that a location produces connector and source data.")
  public void componentData() throws Exception {
    TypedComponentIdentifier typedComponentIdentifier = TypedComponentIdentifier.builder()
        .type(SOURCE)
        .identifier(buildFromStringRepresentation("http:listener"))
        .build();
    ComponentLocation location = mock(ComponentLocation.class);
    when(location.getComponentIdentifier()).thenReturn(typedComponentIdentifier);
    when(location.getParts()).thenReturn(asList(new DefaultLocationPart("flow", empty(), empty(), empty())));
    BaseEventContext context = contextWithComponentLocation.apply(location);

    assertThat(context.getOriginatingLocation().getComponentIdentifier().getIdentifier().getNamespace(), is("http"));
    assertThat(context.getOriginatingLocation().getComponentIdentifier().getIdentifier().getName(), is("listener"));
  }

  @Test
  @Description("Verify that a single component location produces connector and source data.")
  public void componentDataFromSingleComponent() throws Exception {
    BaseEventContext context = this.context.get();

    assertThat(context.getOriginatingLocation().getComponentIdentifier().getIdentifier().getNamespace(), is(CORE_PREFIX));
    assertThat(context.getOriginatingLocation().getComponentIdentifier().getIdentifier().getName(), is("test"));
  }

  private void assertParent(Matcher<Object> eventMatcher, Matcher<Object> errorMatcher, boolean complete, boolean terminated) {
    assertThat(parentResultValue.get(), eventMatcher);
    assertThat(parentErrorValue.get(), errorMatcher);
    assertThat(parentCompletion.get(), is(complete));
    assertThat(parentTerminated.get(), is(terminated));
  }

  private void assertChild(Matcher<Object> eventMatcher, Matcher<Object> errorMatcher, boolean complete) {
    assertThat(childResultValue.get(), eventMatcher);
    assertThat(childErrorValue.get(), errorMatcher);
    assertThat(childCompletion.get(), is(complete));
  }

}
