/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;

/**
 * Implementation of this interfaces must provide functions to convert and {@link CoreEvent} to the response parameters of the success
 * and failure response functions.
 *
 * @since 4.0
 */
public interface OperationParametersProcessor {

  /**
   * Generates the operation function parameters.
   *
   * @return the operation parameters as map
   */
  Map<String, Object> getOperationParameters() throws Exception;

}
