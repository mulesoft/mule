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

  public static final ProcessingStrategy LEGACY_SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE =
      (flowConstruct, pipelineFunction) -> event -> {
        throw new IllegalStateException("Sink is not supported for "
            + LegacySynchronousProcessingStrategyFactory.class.getName());
      };

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return LEGACY_SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
  }
}
