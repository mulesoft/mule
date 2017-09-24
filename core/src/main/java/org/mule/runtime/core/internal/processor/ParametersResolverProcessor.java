/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Implementations provide a means of resolving the parameters that the processor will receive, performing any required value
 * resolution.
 *
 * @since 4.0
 */
public interface ParametersResolverProcessor<T extends ComponentModel> {

  /**
   * Resolve the parameters of this operation before executing it. This is useful to do any handling of any synthetic parameters
   * of an operation before actually calling it.
   *
   * @param eventBuilder a builder for the event to enter the processor for whom parameters are to be resolved
   * @param afterConfigurer the action to perform after resolving the parameters on the builder.
   * @throws MuleException for any exception that occurs while resolving the parameters
   */
  void resolveParameters(CoreEvent.Builder eventBuilder, BiConsumer<Map<String, Object>, ExecutionContext> afterConfigurer)
      throws MuleException;

  /**
   * Perform the required cleanup a the parameters in an {@link ExecutionContext} resolved by calling
   * {@link #resolveParameters(CoreEvent)}.
   * <p>
   * It is mandatory to call this when the parameters are no longer needed when using {@link #resolveParameters(CoreEvent)}.
   *
   * @param executionContext the context that contains the resolved parameters
   */
  void disposeResolvedParameters(ExecutionContext<T> executionContext);

}
