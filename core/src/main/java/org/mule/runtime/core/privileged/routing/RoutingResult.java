/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;


import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.EventProcessingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * The result of routing an {@link CoreEvent} to {@code n} {@link MessageProcessorChain} routes, or {@code n} {@link CoreEvent}'s
 * to the same {@link MessageProcessorChain} route typically by using {@link ForkJoinStrategy}.
 * <p>
 * Results are indexed using the order of {@link RoutingPair} as defined by the router. With
 * {@link org.mule.runtime.core.internal.routing.ScatterGatherRouter} this is the order of routes as defined in configuration.
 *
 * @since 4.0
 */
public final class RoutingResult {

  private final Map<String, Message> successfulRoutesResultMap;
  private final Map<String, Error> failedRoutesErrorMap;
  private Map<String, Pair<Error, EventProcessingException>> failedRoutesErrorWithExceptionMap;

  public RoutingResult(Map<String, Message> successfulRoutesResultMap, Map<String, Error> failedRoutesErrorMap) {
    this.successfulRoutesResultMap = unmodifiableMap(successfulRoutesResultMap);
    this.failedRoutesErrorMap = unmodifiableMap(failedRoutesErrorMap);
    this.failedRoutesErrorWithExceptionMap = emptyMap();
  }

  public static RoutingResult routingResultWithException(Map<String, Message> successfulRoutesResultMap,
                                                         Map<String, Pair<Error, EventProcessingException>> failedRoutesErrorWithExceptionMap) {
    RoutingResult routingResult = new RoutingResult(successfulRoutesResultMap, emptyMap());
    routingResult.setFailedRoutesErrorWithExceptionMap(failedRoutesErrorWithExceptionMap);
    return routingResult;
  }

  private void setFailedRoutesErrorWithExceptionMap(Map<String, Pair<Error, EventProcessingException>> failedRoutesErrorWithExceptionMap) {
    this.failedRoutesErrorWithExceptionMap = failedRoutesErrorWithExceptionMap;
  }

  public Map<String, Message> getResults() {
    return successfulRoutesResultMap;
  }

  public Map<String, Error> getFailures() {
    // provide an error map(Map<String, Error>) using either a simple error map(failedRoutesErrorMap)
    // or an error map with exception info(failedRoutesErrorWithExceptionMap)
    if (!failedRoutesErrorMap.isEmpty()) {
      return failedRoutesErrorMap;
    }
    if (!failedRoutesErrorWithExceptionMap.isEmpty()) {
      return failedRoutesErrorWithExceptionMap.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, pair -> pair.getValue().getFirst()));
    }
    return failedRoutesErrorMap;
  }

  public Map<String, Pair<Error, EventProcessingException>> getFailuresWithExceptionInfo() {
    return failedRoutesErrorWithExceptionMap;
  }

}
