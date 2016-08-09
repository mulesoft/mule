/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.runtime.extension.api.runtime.operation.OperationResultBuilderFactory;

/**
 * Default implementation of {@link OperationResultBuilderFactory}
 *
 * @since 4.0
 */
public final class DefaultOperationResultBuilderFactory extends OperationResultBuilderFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public <Output, A extends Attributes> OperationResult.Builder<Output, A> create() {
    return new DefaultOperationResultBuilder<>();
  }

}
