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
 * Processing strategy used to force legacy 3.x synchronous behaviour that uses a blocking code path.
 */
@Deprecated
public class LegacySynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return (flowConstruct, pipelineFunction) -> {
      throw new IllegalStateException("Sink cannot be created for " + LegacySynchronousProcessingStrategyFactory.class.getName());
    };
  }
}
