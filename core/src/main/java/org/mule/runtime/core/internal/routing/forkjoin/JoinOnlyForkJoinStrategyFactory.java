/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing.forkjoin;

import org.mule.runtime.core.api.InternalEvent;

import java.util.List;
import java.util.function.Function;

/**
 * {@link org.mule.runtime.core.api.routing.ForkJoinStrategy} that:
 * <ul>
 * <li>Performs parallel execution of route pairs subject to {@code maxConcurrency}.
 * <li>Merges variables using a last-wins strategy.
 * <li>Waits for the completion of all routes, with an optional timeout.
 * <li>Emits the same the original input {@link InternalEvent} to the router.
 * <li>Will processor all routes, regardless of errors, and propagating a composite exception where there were one or more errors.
 * </ul>
 */
public class JoinOnlyForkJoinStrategyFactory extends AbstractForkJoinStrategyFactory {

  @Override
  protected Function<List<InternalEvent>, InternalEvent> createResultEvent(InternalEvent original,
                                                                           InternalEvent.Builder resultBuilder) {
    return list -> resultBuilder.build();
  }
}
