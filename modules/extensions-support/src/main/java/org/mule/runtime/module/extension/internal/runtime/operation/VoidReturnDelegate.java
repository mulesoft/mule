/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * An implementation of {@link ReturnDelegate} intended for operations which return {@link Void} and that were executed with a
 * {@link ExecutionContextAdapter}
 * <p/>
 * It returns the {@link CoreEvent} that {@link ExecutionContextAdapter} provides. Notices that this class will fail if used with any
 * other type of {@link ExecutionContext}
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
   * @return {@link ExecutionContextAdapter#getEvent()}
   */
  @Override
  public CoreEvent asReturnValue(Object value, ExecutionContextAdapter operationContext) {
    return CoreEvent.builder(operationContext.getEvent()).securityContext(operationContext.getSecurityContext()).build();
  }
}
