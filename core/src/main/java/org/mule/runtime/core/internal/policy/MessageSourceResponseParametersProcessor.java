/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedFunction;

import java.util.Map;

/**
 * Implementation of this interfaces must provide functions to convert and {@link CoreEvent} to the response parameters of the
 * success and failure response functions.
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
