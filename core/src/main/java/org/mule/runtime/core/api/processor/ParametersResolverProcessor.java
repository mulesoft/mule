/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.util.Map;

/**
 * Implementations provide a means of resolving the parameters that the processor will receive, performing any required value
 * resolution.
 * 
 * @since 4.0
 */
public interface ParametersResolverProcessor {

  /**
   * Resolve the parameters of this operation before executing it. This is useful to do any handling of any synetic parameters of
   * an operation before actually calling it.
   * 
   * @param event the event entering the processor for whom parameters are to be resolved
   * @return the resolved parameters, with the name of the parameter as the key of each entry, along with the
   *         {@link ExecutionContext} of the operation.
   * @throws MuleException for any excpetion that occurs while resolving the parameters
   */
  ParametersResolverProcessorResult resolveParameters(Event event) throws MuleException;

  /**
   * Perform the required cleanup a the parameters in an {@link ExecutionContext} resolved by calling
   * {@link #resolveParameters(Event)}.
   * <p>
   * It is mandatory to call this when the parameters are no longer needed when using {@link #resolveParameters(Event)}.
   * 
   * @param executionContext the context that contains the resolved parameters
   */
  void disposeResolvedParameters(ExecutionContext<OperationModel> executionContext);

  /**
   * Container for the parameters and the {@link ExecutionContext} of an operation.
   */
  class ParametersResolverProcessorResult {

    private final Map<String, Object> parameters;
    private final ExecutionContext context;

    public ParametersResolverProcessorResult(Map<String, Object> parameters, ExecutionContext context) {
      this.parameters = parameters;
      this.context = context;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }

    public ExecutionContext getContext() {
      return context;
    }
  }
}
