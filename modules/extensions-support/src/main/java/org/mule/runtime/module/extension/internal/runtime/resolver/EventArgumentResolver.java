/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;

/**
 * An implementation of {@link ArgumentResolver} which returns the {@link MuleEvent} associated with a given
 * {@link OperationContext}.
 * <p/>
 * Notice that for this to work, the {@link OperationContext} has to be an instance of {@link OperationContextAdapter}
 *
 * @since 3.7.0
 */
public final class EventArgumentResolver implements ArgumentResolver<MuleEvent> {

  /**
   * Returns the {@link MuleEvent} associated to the {@code operationContext}
   *
   * @throws ClassCastException if {@code operationContext} is not an {@link OperationContextAdapter}
   */
  @Override
  public MuleEvent resolve(OperationContext operationContext) {
    return ((OperationContextAdapter) operationContext).getEvent();
  }
}
