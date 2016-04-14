/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.routing.outbound.AbstractOutboundRouter;

/**
 * FirstSuccessful routes an event to the first target route that can accept it
 * without throwing or returning an exception. If no such route can be found, an
 * exception is thrown. Note that this works more reliable with synchronous targets,
 * but no such restriction is imposed.
 */
public class FirstSuccessful extends AbstractOutboundRouter
{

    private RoutingStrategy routingStrategy;
    protected String failureExpression;

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        routingStrategy = new FirstSuccessfulRoutingStrategy(muleContext, failureExpression);
    }

    /**
     * Route the given event to one of our targets
     */
    @Override
    public MuleEvent route(MuleEvent event) throws MessagingException
    {
        try
        {
            return routingStrategy.route(event,getRoutes());
        }
        catch (RoutingFailedMessagingException exception)
        {
            throw new CouldNotRouteOutboundMessageException(event, this);
        }
    }

    @Override
    public boolean isMatch(MuleMessage message) throws MuleException
    {
        return true;
    }

    /**
     * Specifies an expression that when evaluated as determines if the processing of
     * one a route was a failure or not.
     *
     * @param failureExpression
     * @see ExpressionFilter
     */
    public void setFailureExpression(String failureExpression)
    {
        this.failureExpression = failureExpression;
    }
}
