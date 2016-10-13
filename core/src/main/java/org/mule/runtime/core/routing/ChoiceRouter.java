/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.filter.Filter;

import java.util.Collection;
import java.util.Collections;

/**
 * Routes the event to a single<code>MessageProcessor</code> using a {@link Filter} to evaluate the event being processed and find
 * the first route that can be used.
 * <p>
 * If a default route has been configured and no match has been found, the default route will be used. Otherwise it throws a
 * {@link RoutePathNotFoundException}.
 */
public class ChoiceRouter extends AbstractSelectiveRouter implements Processor {

  @Override
  protected Collection<Processor> selectProcessors(Event event, Event.Builder builder) {
    for (MessageProcessorFilterPair mpfp : getConditionalMessageProcessors()) {
      if (mpfp.getFilter().accept(event, builder)) {
        return Collections.singleton(mpfp.getMessageProcessor());
      }
    }

    return Collections.emptySet();
  }
}
