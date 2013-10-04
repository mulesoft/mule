/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.filters.ExpressionFilter;

import java.util.List;

/**
 *
 * Routing strategy that routes the message through a list of {@link MessageProcessor} until
 * one is successfully executed.
 *
 * The message will be route to the first route, if the route execution is successful then
 * execution ends, if not the message will be route to the next route. This continues until a
 * successful route is found.
 */
public class FirstSuccessfulRoutingStrategy extends AbstractRoutingStrategy
{
    protected ExpressionFilter failureExpressionFilter;
    private final MuleContext muleContext;

    /**
     * @param muleContext
     * @param failureExpression Mule expression that validates if a {@link MessageProcessor} execution was successful or not.
     */
    public FirstSuccessfulRoutingStrategy(final MuleContext muleContext, final String failureExpression)
    {
        super(muleContext);
        this.muleContext = muleContext;
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

    @Override
    public MuleEvent route(MuleEvent event, List<MessageProcessor> messageProcessors) throws MessagingException
    {
        MuleEvent returnEvent = null;

        boolean failed = true;
        Exception failExceptionCause = null;
        for (MessageProcessor mp : messageProcessors)
        {
            try
            {
                MuleEvent toProcess = cloneEventForRoutinng(event, mp);
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
                throw new RoutingFailedMessagingException(CoreMessages.createStaticMessage("all message processor failed during first successful routing strategy") ,event, failExceptionCause);
            }
            else
            {
                throw new RoutingFailedMessagingException(CoreMessages.createStaticMessage("all message processor failed during first successful routing strategy") ,event);
            }
        }

        return returnEvent;
    }

    private MuleEvent cloneEventForRoutinng(MuleEvent event, MessageProcessor mp) throws MessagingException
    {
        return createEventToRoute(event, cloneMessage(event, event.getMessage(), muleContext), mp);
    }

}
