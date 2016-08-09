/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.debug;

/**
 * Determine if the {@link ExecutionManager} supports or not dynamic scripting evaluation.
 *
 * @since 3.8.0
 */
public interface ScriptEvaluator {

  /**
   * Evaluates the script and returns the information of the result
   *
   * @param script The script to be evaluated
   * @return The result of the script
   */
  FieldDebugInfo<?> eval(String script);

}
