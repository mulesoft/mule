/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.result;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * A delegate interface to decouple a {@link CompletableComponentExecutor}'s return value from how it is transformed into an
 * {@link CoreEvent} to be handed back into the pipeline
 *
 * @since 4.0
 */
public interface ReturnDelegate {

  /**
   * Adapts the {@code value} into an {@link CoreEvent}
   *
   * @param value            the value to be returned
   * @param operationContext the {@link ExecutionContextAdapter} on which the operation was executed
   * @return a {@link CoreEvent} carrying the operation's result
   */
  CoreEvent asReturnValue(Object value, ExecutionContextAdapter operationContext);
}
