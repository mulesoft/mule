/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedFunction;

import java.util.Map;

/**
 * Implementation of this interfaces must provide functions to convert and {@link CoreEvent} to the response parameters of the success
 * and failure response functions.
 *
 * @since 4.0
 */
public interface MessageSourceResponseParametersProcessor {


  /**
   * Generates the response function parameters. This function is later used to generate the response parameters and use them
   * create a {@code Message} that will be routed through the source policy pipeline.
   *
   * @return a function to resolve the response function parameters.
   */
  CheckedFunction<CoreEvent, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction();

  /**
   * Generates the failure response function parameters. This function is later used to generate the response parameters and use
   * them create a {@code Message} that will be routed through the source policy pipeline.
   *
   * @return a function to resolve the failure response function parameters.
   */
  CheckedFunction<CoreEvent, Map<String, Object>> getFailedExecutionResponseParametersFunction();
}
