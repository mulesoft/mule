/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

/**
 * This processing strategy processes all message processors in the calling thread.
 */
public class SynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  public static ProcessingStrategy SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE = new ProcessingStrategy() {

    @Override
    public boolean isSynchronous() {
      return true;
    }
  };

  @Override
  public ProcessingStrategy create(MuleContext muleContext) {
    return SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
  }

}
