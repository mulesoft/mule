/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public class SdkInternalContextTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("MULE-18189")
  @Description("scatter-gather sends the same event to different routes. In that case, the relationship of what context belongs to the operation on what route must be kept.")
  public void contextSharedOnParallelRoutes() throws MuleException {
    final SdkInternalContext ctx = new SdkInternalContext();

    final Pair<ComponentLocation, String> comp1 = new Pair<>(fromSingleComponent("comp1"), "event1");
    final Pair<ComponentLocation, String> comp2 = new Pair<>(fromSingleComponent("comp2"), "event1");

    final List<Pair<ComponentLocation, String>> completedForComponents = new ArrayList<>();

    pushContext(ctx, comp1, completedForComponents);
    pushContext(ctx, comp2, completedForComponents);

    ctx.getOperationExecutionParams(comp1).getCallback().complete(comp1);
    ctx.getOperationExecutionParams(comp2).getCallback().complete(comp2);

    assertThat(completedForComponents, contains(comp1, comp2));
  }

  @Test
  @Issue("MULE-18227")
  @Description("parallel-foreach sends differenr events to the same routes. In that case, the relationship of what context belongs to the operation on what route must be kept.")
  public void contextSharedOnParallelEvents() throws MuleException {
    final SdkInternalContext ctx = new SdkInternalContext();

    final Pair<ComponentLocation, String> comp1a = new Pair<>(fromSingleComponent("comp1"), "event1");
    final Pair<ComponentLocation, String> comp1b = new Pair<>(fromSingleComponent("comp1"), "event2");

    final List<Pair<ComponentLocation, String>> completedForComponents = new ArrayList<>();

    pushContext(ctx, comp1a, completedForComponents);
    pushContext(ctx, comp1b, completedForComponents);

    ctx.getOperationExecutionParams(comp1a).getCallback().complete(comp1a);
    ctx.getOperationExecutionParams(comp1b).getCallback().complete(comp1b);

    assertThat(completedForComponents, contains(comp1a, comp1b));
  }

  private void pushContext(final SdkInternalContext ctx, final Pair<ComponentLocation, String> location,
                           final List<Pair<ComponentLocation, String>> completedForComponents)
      throws MuleException {
    ctx.putContext(location);
    ctx.setOperationExecutionParams(location, empty(), emptyMap(), testEvent(), new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        completedForComponents.add(location);
      }

      @Override
      public void error(Throwable e) {
        throw new MuleRuntimeException(e);
      }
    });
  }
}
