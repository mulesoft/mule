/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;

/**
 * A delegate interface to decouple a {@link OperationExecutor}'s return value from the format in which it is to be handed back
 * into the pipeline
 *
 * @since 4.0
 */
interface ReturnDelegate {

  /**
   * Adapts the {@code value} to another format
   *
   * @param value the value to be returned
   * @param operationContext the {@link OperationContextAdapter} on which the operation was executed
   * @return a {@link MuleEvent} carrying the operation's result
   */
  MuleEvent asReturnValue(Object value, OperationContextAdapter operationContext);
}
