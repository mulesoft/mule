/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.module.extension.internal.runtime.function.FunctionExecutorFactory;

/**
 * A {@link ModelProperty} meant to be used on {@link OperationModel operation models}, which provides access to a
 * {@link FunctionExecutorFactory} used to execute such operation
 *
 * @since 4.0
 */
public final class FunctionExecutorModelProperty implements ModelProperty {

  public static final String NAME = "functionExecutor";
  private final FunctionExecutorFactory executorFactory;

  /**
   * Creates a new instance
   *
   * @param executorFactory a {@link FunctionExecutorFactory}
   */
  public FunctionExecutorModelProperty(FunctionExecutorFactory executorFactory) {
    this.executorFactory = executorFactory;
  }

  /**
   * @return a {@link FunctionExecutorFactory}
   */
  public FunctionExecutorFactory getExecutorFactory() {
    return executorFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
