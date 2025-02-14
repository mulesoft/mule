/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.forkjoin;

import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextDontComplete;

import static java.util.Optional.empty;

import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import org.reactivestreams.Publisher;

/**
 * Implementation of {@link RoutePairPublisherAssemblyHelper} which does not complete the child contexts on timeout.
 * <p>
 * Exists as a fallback for the kill switch COMPLETE_CHILDREN_ON_TIMEOUT.
 *
 * @implNote This implementation can be removed once the {@link MuleRuntimeFeature#FORK_JOIN_COMPLETE_CHILDREN_ON_TIMEOUT} feature
 *           flag is removed
 */
class LegacyRoutePairPublisherAssemblyHelper implements RoutePairPublisherAssemblyHelper {

  private final Publisher<CoreEvent> publisherWithChildContext;

  LegacyRoutePairPublisherAssemblyHelper(CoreEvent routeEvent, ReactiveProcessor chain) {
    this.publisherWithChildContext = processWithChildContextDontComplete(routeEvent, chain, empty());
  }

  @Override
  public Publisher<CoreEvent> getPublisherOnChildContext() {
    return publisherWithChildContext;
  }

  @Override
  public Publisher<CoreEvent> decorateTimeoutPublisher(Publisher<CoreEvent> timeoutPublisher) {
    // Adds nothing to the timeout publisher
    return timeoutPublisher;
  }
}
