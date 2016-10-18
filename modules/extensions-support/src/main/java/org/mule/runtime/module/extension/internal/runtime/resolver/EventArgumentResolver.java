/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;

/**
 * An implementation of {@link ArgumentResolver} which returns the {@link Event} associated with a given {@link ExecutionContext}.
 * <p/>
 * Notice that for this to work, the {@link ExecutionContext} has to be an instance of {@link ExecutionContextAdapter}
 *
 * @since 3.7.0
 */
public final class EventArgumentResolver implements ArgumentResolver<Event> {

  /**
   * Returns the {@link Event} associated to the {@code operationContext}
   *
   * @throws ClassCastException if {@code operationContext} is not an {@link ExecutionContextAdapter}
   */
  @Override
  public Event resolve(ExecutionContext executionContext) {
    return ((ExecutionContextAdapter) executionContext).getEvent();
  }
}
