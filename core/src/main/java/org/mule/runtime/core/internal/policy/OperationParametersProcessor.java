/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;

/**
 * Implementation of this interfaces must provide functions to convert and {@link CoreEvent} to the response parameters of the
 * success and failure response functions.
 *
 * @since 4.0
 */
public interface OperationParametersProcessor {

  /**
   * Generates the operation function parameters.
   *
   * @return the operation parameters as map
   */
  Map<String, Object> getOperationParameters();

}
