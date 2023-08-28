/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing.forkjoin;

import static org.mule.runtime.api.metadata.DataType.OBJECT;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy;

import java.util.List;
import java.util.function.Function;

/**
 * {@link ForkJoinStrategy} that:
 * <ul>
 * <li>Performs parallel execution of route pairs subject to {@code maxConcurrency}.
 * <li>Merges variables using a last-wins strategy.
 * <li>Waits for the completion of all routes, with an optional timeout.
 * <li>Emits the same the original input {@link CoreEvent} to the router.
 * <li>Will processor all routes, regardless of errors, and propagating a composite exception where there were one or more errors.
 * </ul>
 */
public class JoinOnlyForkJoinStrategyFactory extends AbstractForkJoinStrategyFactory {

  @Override
  protected Function<List<CoreEvent>, CoreEvent> createResultEvent(CoreEvent original,
                                                                   CoreEvent.Builder resultBuilder) {
    return list -> resultBuilder.build();
  }

  @Override
  public DataType getResultDataType() {
    return OBJECT;
  }
}
