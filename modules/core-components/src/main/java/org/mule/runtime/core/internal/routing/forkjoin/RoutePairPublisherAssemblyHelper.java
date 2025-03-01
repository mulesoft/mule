/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.forkjoin;

import org.mule.runtime.core.api.event.CoreEvent;

import org.reactivestreams.Publisher;

/**
 * Helper class to assist in assembling the routing pair publisher.
 */
interface RoutePairPublisherAssemblyHelper {

  Publisher<CoreEvent> getPublisherOnChildContext();

  Publisher<CoreEvent> decorateTimeoutPublisher(Publisher<CoreEvent> timeoutPublisher);
}
