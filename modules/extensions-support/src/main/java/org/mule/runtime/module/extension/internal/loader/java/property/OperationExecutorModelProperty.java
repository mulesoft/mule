/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;

/**
 * A {@link ModelProperty} meant to be used on {@link OperationModel operation models},
 * which provides access to a {@link OperationExecutorFactory} used to execute such
 * operation
 *
 * @since 4.0
 */
public final class OperationExecutorModelProperty implements ModelProperty {

  private final OperationExecutorFactory executorFactory;

  /**
   * Creates a new instance
   *
   * @param executorFactory a {@link OperationExecutorFactory}
   */
  public OperationExecutorModelProperty(
                                        OperationExecutorFactory executorFactory) {
    this.executorFactory = executorFactory;
  }

  /**
   * @return a {@link OperationExecutorFactory}
   */
  public OperationExecutorFactory getExecutorFactory() {
    return executorFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return null;
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
