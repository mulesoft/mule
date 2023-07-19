/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.util.Map;
import java.util.function.Function;

/**
 * Implementations provide a {@link Function} to get the parameters that will be passed to an operation from an
 * {@link OperationModel}.
 * 
 * @since 4.0
 */
public interface OperationArgumentResolverFactory<T extends ComponentModel> {

  /**
   * Builds the argument resolver based on an {@code executionContext} and the internal state of this object.
   */
  Function<ExecutionContext<T>, Map<String, Object>> createArgumentResolver(T operationModel);

}
