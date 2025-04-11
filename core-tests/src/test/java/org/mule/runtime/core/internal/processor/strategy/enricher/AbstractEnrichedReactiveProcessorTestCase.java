/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * General test logic for an enriched {@link ReactiveProcessor}
 *
 * @since 4.4.0
 */
public class AbstractEnrichedReactiveProcessorTestCase {

  protected static final String ARTIFACT_ID = "artifactId";
  protected static final String ARTIFACT_TYPE = "artifactType";
  protected static final ReactiveProcessor reactiveProcessor = p -> p;

  protected void createAndExecuteEnrichedTransformer(ReactiveProcessor transform, CoreEvent coreEvent) {
    Flux<CoreEvent> flux = Flux.just(coreEvent, coreEvent, coreEvent).transform(transform);

    StepVerifier.create(flux)
        .expectNextCount(3)
        .verifyComplete();
  }
}
