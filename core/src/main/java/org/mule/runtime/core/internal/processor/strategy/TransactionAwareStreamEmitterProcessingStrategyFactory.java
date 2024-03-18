/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

/**
 * Creates a processing strategy with same behavior as {@link StreamEmitterProcessingStrategyFactory} apart from the fact it will
 * process synchronously without error when a transaction is active.
 *
 * @since 4.3.0
 */
public class TransactionAwareStreamEmitterProcessingStrategyFactory extends StreamEmitterProcessingStrategyFactory
    implements TransactionAwareProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    TransactionAwareStreamEmitterProcessingStrategyDecorator psDecorator =
        new TransactionAwareStreamEmitterProcessingStrategyDecorator(super.create(muleContext, schedulersNamePrefix));

    try {
      initialiseIfNeeded(psDecorator, muleContext);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }

    return psDecorator;
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return TransactionAwareStreamEmitterProcessingStrategyDecorator.class;
  }
}
