/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing;

import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.routing.AggregationContext;

/**
 * Strategy pattern for aggregating a list of {@link InternalEvent} passed through a {@link AggregationContext} into a single one
 * 
 * @since 3.5.0
 */
public interface AggregationStrategy {

  /**
   * aggregates the events in the given context into a single one
   * 
   * @param context an instance of {@link AggregationContext}
   * @return a resulting {@link InternalEvent}. It can be a new event, an enriched version of
   *         {@link AggregationContext#getOriginalEvent()} or whatever makes sense in your use case. It cannot be
   *         <code>null</code>, return {@link VoidMuleEvent} instead
   * @throws MuleException if unexpected exception occurs or business error condition is met
   */
  InternalEvent aggregate(AggregationContext context) throws MuleException;

}
