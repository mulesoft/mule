/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;

import java.util.Collection;
import java.util.Collections;

/**
 * Routes the event to a single<code>MessageProcessor</code> using a {@link Filter} to evaluate the event being processed and find
 * the first route that can be used.
 * <p>
 * If a default route has been configured and no match has been found, the default route will be used. Otherwise it throws a
 * {@link RoutePathNotFoundException}.
 */
public class ChoiceRouter extends AbstractSelectiveRouter implements NonBlockingMessageProcessor {

  @Override
  protected Collection<MessageProcessor> selectProcessors(MuleEvent event) {
    for (MessageProcessorFilterPair mpfp : getConditionalMessageProcessors()) {
      if (mpfp.getFilter().accept(event)) {
        return Collections.singleton(mpfp.getMessageProcessor());
      }
    }

    return Collections.emptySet();
  }
}
