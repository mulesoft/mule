/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
