/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * A delegate interface to decouple a {@link CompletableComponentExecutor}'s return value from how it is transformed
 * into an {@link CoreEvent} to be handed back into the pipeline
 *
 * @since 4.0
 */
interface ReturnDelegate {

  /**
   * Adapts the {@code value} into an {@link CoreEvent}
   *
   * @param value the value to be returned
   * @param operationContext the {@link ExecutionContextAdapter} on which the operation was executed
   * @return a {@link CoreEvent} carrying the operation's result
   */
  CoreEvent asReturnValue(Object value, ExecutionContextAdapter operationContext);
}
