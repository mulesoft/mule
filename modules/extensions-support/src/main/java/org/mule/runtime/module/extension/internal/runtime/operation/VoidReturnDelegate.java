/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.extension.api.runtime.operation.OperationContext;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;

/**
 * An implementation of {@link ReturnDelegate} intended for operations which return {@link Void} and that were executed with a
 * {@link OperationContextAdapter}
 * <p/>
 * It returns the {@link MuleEvent} that {@link OperationContextAdapter} provides. Notices that this class will fail if used with
 * any other type of {@link OperationContext}
 * <p/>
 * This class is intended to be used as a singleton, use the {@link #INSTANCE} attribute to access the instance
 *
 * @since 3.7.0
 */
final class VoidReturnDelegate implements ReturnDelegate {

  static final ReturnDelegate INSTANCE = new VoidReturnDelegate();

  private VoidReturnDelegate() {}

  /**
   * {@inheritDoc}
   * 
   * @return {@link OperationContextAdapter#getEvent()}
   */
  @Override
  public MuleEvent asReturnValue(Object value, OperationContextAdapter operationContext) {
    return operationContext.getEvent();
  }
}
