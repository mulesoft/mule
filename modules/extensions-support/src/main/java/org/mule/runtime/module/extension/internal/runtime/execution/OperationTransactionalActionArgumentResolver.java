/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
