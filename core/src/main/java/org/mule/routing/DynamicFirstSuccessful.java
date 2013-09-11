/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;


/**
 *
 * Routes a message through a set of routes that will be obtained
 * dynamically (per message) using a {@link DynamicRouteResolver}.
 *
 * The message will be route to the first route, if the route execution is successful then
 * execution ends, if not the message will be route to the next route. This continues until a
 * successful route is found.
 *
 */
public class DynamicFirstSuccessful  implements MessageProcessor, Initialisable, MuleContextAware
{
    private FirstSuccessfulRoutingStrategy routingStrategy;
    private MuleContext muleContext;
    private String failureExpression;
    private DynamicRouteResolver dynamicRouteResolver;


    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return routingStrategy.route(event, dynamicRouteResolver.resolveRoutes(event));
    }

    @Override
    public void initialise() throws InitialisationException
    {
        routingStrategy = new FirstSuccessfulRoutingStrategy(muleContext, failureExpression);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    /**
     * Specifies an expression that when evaluated as determines if the processing of
     * one a route was a failure or not.
     *
     * @param failureExpression
     * @see org.mule.routing.filters.ExpressionFilter
     */
    public void setFailureExpression(String failureExpression)
    {
        this.failureExpression = failureExpression;
    }

    /**
     * @param dynamicRouteResolver custom route resolver to use
     */
    public void setDynamicRouteResolver(DynamicRouteResolver dynamicRouteResolver)
    {
        this.dynamicRouteResolver = dynamicRouteResolver;
    }
}
