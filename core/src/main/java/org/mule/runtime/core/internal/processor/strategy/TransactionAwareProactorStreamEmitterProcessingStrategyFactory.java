/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.StreamEmitterProcessingStrategyFactory.StreamEmitterProcessingStrategy;

/**
 * Creates a processing strategy with same behavior as {@link ProactorStreamEmitterProcessingStrategyFactory} apart from the fact
 * it will process synchronously without error when a transaction is active.
 *
 * @since 4.0
 */
public class TransactionAwareProactorStreamEmitterProcessingStrategyFactory extends ProactorStreamEmitterProcessingStrategyFactory
    implements TransactionAwareProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new TransactionAwareStreamEmitterProcessingStrategyDecorator(super.create(muleContext, schedulersNamePrefix));
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return TransactionAwareStreamEmitterProcessingStrategyDecorator.class;
  }
}
