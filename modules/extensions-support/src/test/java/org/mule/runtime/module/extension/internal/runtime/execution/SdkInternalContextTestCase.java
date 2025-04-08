/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public class SdkInternalContextTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  @Issue("MULE-18189")
  @Description("scatter-gather sends the same event to different routes. In that case, the relationship of what context belongs to the operation on what route must be kept.")
  public void contextSharedOnParallelRoutes() throws MuleException {
    final SdkInternalContext ctx = new SdkInternalContext();

    final ComponentLocation comp1 = from("comp1");
    final ComponentLocation comp2 = from("comp2");

    final List<Pair<ComponentLocation, String>> completedForComponents = new ArrayList<>();

    pushContext(ctx, comp1, "event1", completedForComponents);
    pushContext(ctx, comp2, "event1", completedForComponents);

    ctx.getOperationExecutionParams(comp1, "event1").getCallback().complete(comp1);
    ctx.getOperationExecutionParams(comp2, "event1").getCallback().complete(comp2);

    assertThat(completedForComponents, contains(new Pair<>(comp1, "event1"), new Pair<>(comp2, "event1")));
  }

  @Test
  @Issue("MULE-18227")
  @Description("parallel-foreach sends differenr events to the same routes. In that case, the relationship of what context belongs to the operation on what route must be kept.")
  public void contextSharedOnParallelEvents() throws MuleException {
    final SdkInternalContext ctx = new SdkInternalContext();

    final ComponentLocation comp1 = from("comp1");

    final List<Pair<ComponentLocation, String>> completedForComponents = new ArrayList<>();

    pushContext(ctx, comp1, "event1", completedForComponents);
    pushContext(ctx, comp1, "event2", completedForComponents);

    ctx.getOperationExecutionParams(comp1, "event1").getCallback().complete(comp1);
    ctx.getOperationExecutionParams(comp1, "event2").getCallback().complete(comp1);

    assertThat(completedForComponents, contains(new Pair<>(comp1, "event1"), new Pair<>(comp1, "event2")));
  }

  @Test
  @Issue("W-17980769")
  public void putCollidingKeys() {
    final SdkInternalContext ctx = new SdkInternalContext();

    final ComponentLocation comp1 = from("comp1");

    ctx.putContext(comp1, "event1");
    final ConfigurationInstance configInstance = mock(ConfigurationInstance.class);
    ctx.setConfiguration(comp1, "event1", of(configInstance));

    expected.expect(IllegalStateException.class);
    try {
      ctx.putContext(comp1, "event1");
    } finally {
      assertThat("The failed call to `putContext` overwrote the original context",
                 ctx.getConfiguration(comp1, "event1").get(), sameInstance(configInstance));
    }
  }

  @Test
  @Issue("W-17980769")
  public void putOperationExecutionParamsTwice() throws MuleException {
    final SdkInternalContext ctx = new SdkInternalContext();

    final ComponentLocation comp1 = from("comp1");

    ctx.putContext(comp1, "event1");
    ctx.setOperationExecutionParams(comp1, "event1", empty(), emptyMap(), testEvent(), null, null);

    expected.expect(IllegalStateException.class);
    ctx.setOperationExecutionParams(comp1, "event1", empty(), emptyMap(), testEvent(), null, null);
  }

  @Test
  @Issue("W-17980769")
  public void putOperationExecutionWithoutContext() throws MuleException {
    final SdkInternalContext ctx = new SdkInternalContext();

    final ComponentLocation comp1 = from("comp1");

    expected.expect(NullPointerException.class);
    ctx.setOperationExecutionParams(comp1, TEST_CONNECTOR, empty(), emptyMap(), testEvent(), null, null);
  }

  private void pushContext(final SdkInternalContext ctx, ComponentLocation location, String eventId,
                           final List<Pair<ComponentLocation, String>> completedForComponents)
      throws MuleException {
    ctx.putContext(location, eventId);
    ctx.setOperationExecutionParams(location, eventId, empty(), emptyMap(), testEvent(), new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        completedForComponents.add(new Pair<>(location, eventId));
      }

      @Override
      public void error(Throwable e) {
        throw new MuleRuntimeException(e);
      }
    }, null);
  }
}
