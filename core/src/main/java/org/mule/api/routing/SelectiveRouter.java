/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
