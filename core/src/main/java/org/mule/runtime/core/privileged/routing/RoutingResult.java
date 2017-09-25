/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;


import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy;

import java.util.Map;

/**
 * The result of routing an {@link CoreEvent} to {@code n} {@link MessageProcessorChain} routes, or {@code n}
 * {@link CoreEvent}'s to the same {@link MessageProcessorChain} route typically by using {@link ForkJoinStrategy}.
 * <p>
 * Results are indexed using the order of {@link RoutingPair} as defined by the router. With
 * {@link org.mule.runtime.core.internal.routing.ScatterGatherRouter} this is the order of routes as defined in configuration.
 *
 * @since 4.0
 */
public final class RoutingResult {

  private final Map<String, Message> successfulRoutesResultMap;
  private final Map<String, Error> failedRoutesErrorMap;

  public RoutingResult(Map<String, Message> successfulRoutesResultMap, Map<String, Error> failedRoutesErrorMap) {
    this.successfulRoutesResultMap = unmodifiableMap(successfulRoutesResultMap);
    this.failedRoutesErrorMap = unmodifiableMap(failedRoutesErrorMap);
  }

  public Map<String, Message> getResults() {
    return successfulRoutesResultMap;
  }

  public Map<String, Error> getFailures() {
    return failedRoutesErrorMap;
  }

}
