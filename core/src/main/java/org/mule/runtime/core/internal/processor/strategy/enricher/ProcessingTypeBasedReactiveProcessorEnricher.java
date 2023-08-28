/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ReactiveProcessorEnricher} that enriches a {@link ReactiveProcessor} with some logic based on its
 * {@link ProcessingType}.
 * 
 * @since 4.4.0
 */
public class ProcessingTypeBasedReactiveProcessorEnricher implements ReactiveProcessorEnricher {

  private final ReactiveProcessorEnricher defaultEnricher;

  private final Map<ProcessingType, ReactiveProcessorEnricher> enrichers =
      new HashMap<ProcessingType, ReactiveProcessorEnricher>();

  public ProcessingTypeBasedReactiveProcessorEnricher(ReactiveProcessorEnricher defaultEnricher) {
    this.defaultEnricher = defaultEnricher;
  }

  @Override
  public ReactiveProcessor enrich(ReactiveProcessor processor) {
    return enrichers.getOrDefault(processor.getProcessingType(), defaultEnricher).enrich(processor);
  }

  public ProcessingTypeBasedReactiveProcessorEnricher register(ProcessingType processingType,
                                                               ReactiveProcessorEnricher enricher) {
    enrichers.put(processingType, enricher);
    return this;
  }

}
