/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor;
import org.mule.runtime.core.processor.LaxAsyncInterceptingMessageProcessor;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategyFactory.AsynchronousProcessingStrategy;

/**
 * This factory's processing strategy uses the 'asynchronous' strategy where possible, but if an event is synchronous it processes
 * it synchronously rather than failing.
 */
public class DefaultFlowProcessingStrategyFactory implements ProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create() {
    return new DefaultFlowProcessingStrategy();
  }

  public static class DefaultFlowProcessingStrategy extends AsynchronousProcessingStrategy {

    @Override
    protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor() {
      return new LaxAsyncInterceptingMessageProcessor();
    }

  }

}
