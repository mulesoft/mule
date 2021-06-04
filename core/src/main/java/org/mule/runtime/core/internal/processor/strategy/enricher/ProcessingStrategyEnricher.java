/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;

import java.util.Optional;
import java.util.Set;

/**
 * Chain of responsibility that enriches a {@link ReactiveProcessor} according to its {@link ProcessingType} determining how the
 * processing strategy will be applied.
 *
 * @since 4.4.0, 4.3.1
 */
public interface ProcessingStrategyEnricher {

  /**
   * @return next @{@link ProcessingStrategyEnricher} in the chain responsibility.
   */
  Optional<ProcessingStrategyEnricher> nextCustomizer();

  /**
   * @param processor The {@link ReactiveProcessor} into which the processing strategy will be applied.
   *
   * @return the enriched {@link ReactiveProcessor}.
   */
  ReactiveProcessor enrich(ReactiveProcessor processor);

  /**
   * @return the {@link ProcessingType} to which this customizer applies.
   */
  Set<ProcessingType> getProcessingTypes();

}
