/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.processor.strategy;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.MuleContext;

/**
 * Abstract Factory for creating concrete {@link ProcessingStrategy}ies.
 *
 * @since 4.0
 */
@NoImplement
public interface ProcessingStrategyFactory {

  /**
   * @return a new {@link ProcessingStrategy}.
   */
  ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix);

  /**
   * Provides a hint to users of the factory about the concrete type of {@link ProcessingStrategy} to be built.
   *
   * @return a reference to the concrete implementation of the {@link ProcessingStrategy} this factory will create.
   */
  default Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return ProcessingStrategy.class;
  }

}
