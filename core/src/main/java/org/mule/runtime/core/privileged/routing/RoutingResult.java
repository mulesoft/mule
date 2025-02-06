/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;


import static org.mule.runtime.api.exception.ExceptionHelper.getRootMuleException;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.EventProcessingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.Map;
import java.util.Map.Entry;
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
  private Map<String, Pair<Error, MuleException>> failedRoutesErrorWithExceptionMap;

  public RoutingResult(Map<String, Message> successfulRoutesResultMap, Map<String, Error> failedRoutesErrorMap) {
    this.successfulRoutesResultMap = unmodifiableMap(successfulRoutesResultMap);
    this.failedRoutesErrorMap = unmodifiableMap(failedRoutesErrorMap);
    this.failedRoutesErrorWithExceptionMap = emptyMap();
  }

  public static RoutingResult routingResultWithException(Map<String, Message> successfulRoutesResultMap,
                                                         Map<String, Pair<Error, EventProcessingException>> failedRoutesErrorWithExceptionMap) {
    RoutingResult routingResult = new RoutingResult(successfulRoutesResultMap, emptyMap());
    Map<String, Pair<Error, MuleException>> failedRoutesErrorWithMuleExceptionMap =
        failedRoutesErrorWithExceptionMap.entrySet().stream()
            .collect(Collectors
                .toMap(Entry::getKey,
                       pair -> new Pair<>(pair.getValue().getFirst(), getRootMuleException(pair.getValue().getSecond()))));
    routingResult.setFailedRoutesErrorWithMuleExceptionMap(failedRoutesErrorWithMuleExceptionMap);
    return routingResult;
  }

  private void setFailedRoutesErrorWithMuleExceptionMap(Map<String, Pair<Error, MuleException>> failedRoutesErrorWithExceptionMap) {
    this.failedRoutesErrorWithExceptionMap = failedRoutesErrorWithExceptionMap;
  }

  public Map<String, Message> getResults() {
    return successfulRoutesResultMap;
  }

  public Map<String, Error> getFailures() {
    if (!failedRoutesErrorWithExceptionMap.isEmpty()) {
      return failedRoutesErrorWithExceptionMap.entrySet().stream()
          .collect(Collectors.toMap(Entry::getKey, pair -> pair.getValue().getFirst()));
    } else {
      return failedRoutesErrorMap;
    }
  }

  public Map<String, Pair<Error, MuleException>> getFailuresWithExceptionInfo() {
    return failedRoutesErrorWithExceptionMap;
  }

}
