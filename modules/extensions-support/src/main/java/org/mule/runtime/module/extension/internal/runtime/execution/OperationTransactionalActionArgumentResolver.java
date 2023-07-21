/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;

/**
 * {@link ArgumentResolver} implementation for {@link OperationTransactionalAction} parameters
 *
 * @since 4.0
 */
public class OperationTransactionalActionArgumentResolver implements ArgumentResolver<OperationTransactionalAction> {

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationTransactionalAction resolve(ExecutionContext executionContext) {
    return (OperationTransactionalAction) executionContext.getParameter(TRANSACTIONAL_ACTION_PARAMETER_NAME);
  }
}
