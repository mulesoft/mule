/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.visitor;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.event.DefaultEventContext;

/**
 * A visitor for {@link EventContext}.
 *
 * @since 4.5.0.
 */
public interface EventContextVisitor<T> {

  /**
   * @param eventContext the {@link DefaultEventContext} to visit.
   * @return the result of visiting the {@link EventContext}.
   */
  T visit(DefaultEventContext eventContext);

  /**
   * @param eventContext the {@link EventContext} to visit.
   * @return the result of visiting the {@link EventContext}.
   */
  T visit(EventContext eventContext);
}
