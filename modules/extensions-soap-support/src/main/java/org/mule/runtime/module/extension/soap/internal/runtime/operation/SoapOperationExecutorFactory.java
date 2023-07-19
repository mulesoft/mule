/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.operation;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;

import java.util.Map;

/**
 * {@link CompletableComponentExecutorFactory} that creates instances of {@link SoapOperationExecutor}.
 */
public final class SoapOperationExecutorFactory implements CompletableComponentExecutorFactory<OperationModel> {

  /**
   * Creates a new executor for soap operations.
   *
   * @param operationModel the model of the operation to be executed
   * @param parameters     parameters for initializing the executor
   * @return a new {@link SoapOperationExecutor}
   */

  @Override
  public CompletableComponentExecutor<OperationModel> createExecutor(OperationModel operationModel,
                                                                     Map<String, Object> parameters) {
    return new SoapOperationExecutor();
  }
}
