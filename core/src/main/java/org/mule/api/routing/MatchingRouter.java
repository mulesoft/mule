/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.routing;

import org.mule.api.processor.MessageProcessor;

/**
 * Routes the message to zero or more <code>MatchableMessageProcessor</code>s. Which
 * route(s) are used used is defined by implementations of this interface which use
 * the {@link Matchable#isMatch(org.mule.api.MuleMessage)} method of the routers to
 * determine if a router accept the event or not.
 * <p>
 * Different implementations may route to the first match, or to all matches or you
 * some other strategy. If the implementation supports the use of a default route
 * then this will be used to route any events that don't match any other routes.
 */
public interface MatchingRouter extends MessageProcessor
{
    void addRoute(MatchableMessageProcessor matchable);

    void removeRoute(MatchableMessageProcessor matchable);
}
