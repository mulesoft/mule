/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.routing;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;


public interface ForkJoinStrategyFactory {

  /**
   *
   * @param processingStrategy
   * @param maxConcurrency
   * @param delayErrors
   * @param timeoutErrorType
   * @return
   */
  ForkJoinStrategy createForkJoinStrategy(ProcessingStrategy processingStrategy, int maxConcurrency, boolean delayErrors,
                                          long timeout,
                                          ErrorType timeoutErrorType);

}
