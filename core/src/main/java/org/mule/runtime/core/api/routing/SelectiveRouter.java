/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.routing;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;

/**
 * Routes the event to <code>MessageProcessor</code>s using a {@link Filter} to
 * evaluate the event being processed and determine if a given route should be used.
 * <p>
 * If the implementation supports the use of a default route then this will be used
 * to route any events that don't match any other routes.
 */
public interface SelectiveRouter extends MessageProcessor
{
    void addRoute(MessageProcessor processor, Filter filter);

    void updateRoute(MessageProcessor processor, Filter filter);

    void removeRoute(MessageProcessor processor);

    void setDefaultRoute(MessageProcessor processor);
}
