/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.mule.runtime.module.extension.internal.runtime.operation.adapter.SdkOperationTransactionalActionUtils.from;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.sdk.api.tx.OperationTransactionalAction;

/**
 * {@link ArgumentResolver} implementation for {@link OperationTransactionalAction} parameters which delegates to a
 * {@link OperationTransactionalActionArgumentResolver} and adapts the result.
 *
 * @since 4.0
 */
public class SdkOperationTransactionalActionArgumentResolver implements ArgumentResolver<OperationTransactionalAction> {

  private final ArgumentResolver<org.mule.runtime.extension.api.tx.OperationTransactionalAction> operationTransactionalActionArgumentResolver =
      new OperationTransactionalActionArgumentResolver();

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationTransactionalAction resolve(ExecutionContext executionContext) {
    org.mule.runtime.extension.api.tx.OperationTransactionalAction operationTransactionalAction =
        operationTransactionalActionArgumentResolver.resolve(executionContext);
    return operationTransactionalAction == null ? null : from(operationTransactionalAction);
  }
}
