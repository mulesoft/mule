/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.enricher;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

/**
 * General test logic for an enriched {@link ReactiveProcessor}
 * 
 * @since 4.4.0
 */
public class AbstractEnrichedReactiveProcessorTestCase {

  protected void createAndExecuteEnrichedTransformer(ReactiveProcessor transform, CoreEvent coreEvent) {
    TestPublisher<CoreEvent> testPublisher = TestPublisher.create();

    Flux<CoreEvent> flux = testPublisher.flux().transform(transform);

    StepVerifier.create(flux)
        .then(() -> testPublisher.emit(coreEvent, coreEvent, coreEvent))
        .expectNextCount(3)
        .verifyComplete();
  }
}
