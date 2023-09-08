/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.error;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.tracer.api.span.InternalSpanCallStack;

/**
 * Default implementation of an {@link InternalSpanCallStack} that wraps the a {@link FlowCallStack}
 *
 * @since 4.5.0
 */
public class DefaultSpanCallStack implements InternalSpanCallStack {

  private final String flowStack;

  public DefaultSpanCallStack(FlowCallStack flowStack) {
    this.flowStack = flowStack.toString();
  }

  @Override
  public String toString() {
    return flowStack;
  }
}
