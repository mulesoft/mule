/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * Enricher for a {@link ReactiveProcessor}.
 *
 * @since 4.4.0
 */
public interface ReactiveProcessorEnricher {

  /**
   * @param processor The {@link ReactiveProcessor} to be enriched.
   *
   * @return the enriched {@link ReactiveProcessor}.
   */
  ReactiveProcessor enrich(ReactiveProcessor processor);

}
