/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.DefaultMuleException;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.message.DefaultExceptionPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link EventMergeStrategy} for the
 * {@link ScatterGatherRouter}. It merges the events by using a
 * {@link DefaultRouterResultsHandler} and throws a {@link CompositeRoutingException}
 * in case of failure
 * 
 * @since 3.5.0
 */
public class ScatterGatherEventMergeStrategy implements EventMergeStrategy
{

    private RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();

    @Override
    public MuleEvent merge(MuleEvent originalEvent, List<MuleEvent> events) throws MuleException
    {
        MuleEvent response = this.resultsHandler.aggregateResults(events, originalEvent,
            originalEvent.getMuleContext());

        Map<Integer, Exception> exceptions = this.collectExceptions(events);
        if (exceptions.isEmpty())
        {
            return response;
        }
        else
        {
            originalEvent.getMessage().setPayload(response.getMessage().getPayload());

            MuleException exception = new CompositeRoutingException(this.buildExceptionMessage(exceptions),
                originalEvent, exceptions);
            originalEvent.getMessage().setExceptionPayload(new DefaultExceptionPayload(exception));

            throw exception;
        }
    }

    private Message buildExceptionMessage(Map<Integer, Exception> exceptions)
    {
        StringBuilder builder = new StringBuilder();
        for (Integer route : exceptions.keySet())
        {
            if (builder.length() > 0)
            {
                builder.append(", ");
            }

            builder.append(route);
        }

        builder.insert(0, "Exception was found for route(s): ");
        return MessageFactory.createStaticMessage(builder.toString());
    }

    private Map<Integer, Exception> collectExceptions(List<MuleEvent> events)
    {
        Map<Integer, Exception> exceptions = new LinkedHashMap<Integer, Exception>(events.size());
        for (MuleEvent event : events)
        {
            ExceptionPayload ep = event.getMessage().getExceptionPayload();
            if (ep != null && ep.getException() != null)
            {
                Integer routeIndex = (Integer) ep.getInfo().get(
                    MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
                Throwable t = ep.getException();
                Exception e = t instanceof Exception ? (Exception) t : new DefaultMuleException(t);
                exceptions.put(routeIndex, e);
            }
        }

        return exceptions;
    }
}
