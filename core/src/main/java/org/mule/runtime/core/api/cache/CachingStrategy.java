/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.cache;

import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.internal.util.rx.Operators.nullSafeMap;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * Defines a way to process a {@link BaseEvent} using a cache.
 */
public interface CachingStrategy {

  /**
   * Processes a {@link BaseEvent} using a caching schema. Uses a message processor to process the request when it is not found in the
   * cache or when it must be processed without using the cache.
   * <p/>
   * Different calls to this method using the same request does not implies that the same instance will be returned. Each
   * implementation could choose to create new instances every time.
   *
   * @param request the event to process
   * @param messageProcessor the message processor that will be executed when the response for the event is not in the cache.
   * @return a response for the request that could be obtained using the cache.
   * @throws MuleException
   */
  BaseEvent process(BaseEvent request, Processor messageProcessor) throws MuleException;


  /**
   * Obtain the publisher function for caching strategy given a processor
   *
   * @param processor the processor that will be executed when the response for the event is not in the cache.
   * @return publisher function
   */
  default ReactiveProcessor transformProcessor(Processor processor) {
    return publisher -> from(publisher).handle(nullSafeMap(checkedFunction(request -> process(request, processor))));
  }
}
