/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
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

    protected String failureExpression;
    protected ExpressionFilter failureExpressionFilter;

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (failureExpression != null)
        {
            failureExpressionFilter = new ExpressionFilter(failureExpression);
        }
        else
        {
            failureExpressionFilter = new ExpressionFilter("exception-type:");
        }
        failureExpressionFilter.setMuleContext(muleContext);
    }

    /**
     * Route the given event to one of our targets
     */
    @Override
    public MuleEvent route(MuleEvent event) throws MessagingException
    {
        MuleEvent returnEvent = null;

        boolean failed = true;
        Exception failExceptionCause = null;
        for (MessageProcessor mp : routes)
        {
            try
            {
                MuleEvent toProcess = cloneEventForRouting(event, mp);
                returnEvent = mp.process(toProcess);

                if (returnEvent == null || VoidMuleEvent.getInstance().equals(returnEvent))
                {
                    failed = false;
                }
                else
                {
                    MuleMessage msg = returnEvent.getMessage();
                    failed = msg == null || failureExpressionFilter.accept(msg);
                }
            }
            catch (Exception ex)
            {
                failed = true;
                failExceptionCause = ex;
            }
            if (!failed)
            {
                break;
            }
        }

        if (failed)
        {
            if (failExceptionCause != null)
            {
                throw new CouldNotRouteOutboundMessageException(event, this, failExceptionCause);
            }
            else
            {
                throw new CouldNotRouteOutboundMessageException(event, this);
            }
        }

        return returnEvent;
    }

    protected MuleEvent cloneEventForRouting(MuleEvent event, MessageProcessor mp) throws MessagingException
    {
        return createEventToRoute(event, cloneMessage(event, event.getMessage()), mp);
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
