/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.filter.Filter;

import java.util.Collection;

/**
 * Routes the event to a single<code>MessageProcessor</code> using a {@link Filter} to evaluate the event being processed and find
 * the first route that can be used.
 * <p>
 * If a default route has been configured and no match has been found, the default route will be used. Otherwise it continues the
 * execution through the next MP in the chain.
 */
public class ChoiceRouter extends AbstractSelectiveRouter {

  @Override
  protected Collection<Processor> selectProcessors(Event event, Event.Builder builder) {
    for (MessageProcessorFilterPair mpfp : getConditionalMessageProcessors()) {
      if (mpfp.getFilter().accept(event, builder)) {
        return singleton(mpfp.getMessageProcessor());
      }
    }

    return emptySet();
  }

  @Override
  protected Collection<Processor> getProcessorsToRoute(Event event) throws RoutePathNotFoundException {
    try {
      return super.getProcessorsToRoute(event);
    } catch (RoutePathNotFoundException e) {
      return singletonList(event1 -> event1);
    }
  }
}
