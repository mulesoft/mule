/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.LegacyAsynchronousProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.LegacyNonBlockingProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.LegacySynchronousProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory;

public class ProcessingStrategyUtils {

  public static String DEFAULT_PROCESSING_STRATEGY = "default";
  public static String SYNC_PROCESSING_STRATEGY = "synchronous";
  public static String NON_BLOCKING_PROCESSING_STRATEGY = "non-blocking";
  public static String ASYNC_PROCESSING_STRATEGY = "asynchronous";

  public static ProcessingStrategyFactory parseProcessingStrategy(String processingStrategy) {
    if (DEFAULT_PROCESSING_STRATEGY.equals(processingStrategy)) {
      return new DefaultFlowProcessingStrategyFactory();
    } else if (SYNC_PROCESSING_STRATEGY.equals(processingStrategy)) {
      return new LegacySynchronousProcessingStrategyFactory();
    } else if (NON_BLOCKING_PROCESSING_STRATEGY.equals(processingStrategy)) {
      return new LegacyNonBlockingProcessingStrategyFactory();
    } else if (ASYNC_PROCESSING_STRATEGY.equals(processingStrategy)) {
      return new LegacyAsynchronousProcessingStrategyFactory();
    }
    return null;
  }

}
