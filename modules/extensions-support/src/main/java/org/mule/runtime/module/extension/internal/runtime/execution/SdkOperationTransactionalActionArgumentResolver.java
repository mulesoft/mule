/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.sdk.api.tx.OperationTransactionalAction;

/**
 * {@link ArgumentResolver} implementation for {@link OperationTransactionalAction} parameters which delegates to a
 * {@link OperationTransactionalActionArgumentResolver} and adapts the result.
 *
 * @since 4.5
 */
public class SdkOperationTransactionalActionArgumentResolver implements ArgumentResolver<OperationTransactionalAction> {

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationTransactionalAction resolve(ExecutionContext executionContext) {
    return (OperationTransactionalAction) executionContext.getParameter(TRANSACTIONAL_ACTION_PARAMETER_NAME);
  }
}
