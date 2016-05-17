/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessageCollection;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.util.CollectionUtils;

import java.util.List;

import org.apache.commons.collections.Predicate;

/**
 * The default results handler for all outbound endpoint. Depending on the number of messages passed it the
 * returning message will be different. If the 'results' param is null or empty, null is returned. If the
 * 'results' param contains a single {@link org.mule.api.MuleMessage}, than that message is returned. If the
 * 'results' param contains more than one message a {@link org.mule.api.MuleMessageCollection} instance is
 * returned.
 * <p/>
 * Note that right now (as of Mule 2.0.1) this SPI is not pluggable and this implementation is the default and
 * only implementation.
 *
 * @see org.mule.api.MuleMessageCollection
 * @see org.mule.api.MuleMessage
 * @see org.mule.DefaultMessageCollection
 */
public class DefaultRouterResultsHandler implements RouterResultsHandler
{
    private boolean returnCollectionWithSingleResult = false;

    public DefaultRouterResultsHandler()
    {
    }

    /**
     * @param returnCollectionWithSingleResult if a MuleMessageCollection should be return despite there's only one result event
     */
    public DefaultRouterResultsHandler(boolean returnCollectionWithSingleResult)
    {
        this.returnCollectionWithSingleResult = returnCollectionWithSingleResult;
    }

    /**
     * Aggregates the events in the results list into one single {@link org.mule.api.MuleEvent}
     * You should only use this method when you're sure that all the events in the results list
     * were generated by the same thread that's going to handle the aggregated response
     *
     * @param results
     * @param previous
     * @param muleContext
     * @return
     */
    @SuppressWarnings(value = {"unchecked"})
    public MuleEvent aggregateResults(final List<MuleEvent> results,
                                      final MuleEvent previous,
                                      MuleContext muleContext)
    {
        if (results == null)
        {
            return null;
        }
        else if (results.size() == 1)
        {
            MuleEvent event = results.get(0);
            if (event == null || event instanceof VoidMuleEvent)
            {
                return event;
            }
            else if (event != null && event.getMessage() != null)
            {
                if (returnCollectionWithSingleResult)
                {
                    return createMessageCollectionWithSingleMessage(event,muleContext);
                }
                else
                {
                    return event;
                }
            }
            else
            {
                return VoidMuleEvent.getInstance();
            }
        }
        else
        {
            List<MuleEvent> nonNullResults = (List<MuleEvent>) CollectionUtils.select(results,
                new Predicate()
                {
                    public boolean evaluate(Object object)
                    {
                        return
                                !VoidMuleEvent.getInstance().equals(object) &&
                                object != null &&
                                ((MuleEvent) object).getMessage() != null;
                    }
                });

            if (nonNullResults.size() == 0)
            {
                return VoidMuleEvent.getInstance();
            }
            else if (nonNullResults.size() == 1)
            {
                return nonNullResults.get(0);
            }
            else
            {
                return createMessageCollection(nonNullResults, previous, muleContext);
            }
        }
    }

    private MuleEvent createMessageCollectionWithSingleMessage(MuleEvent event, MuleContext muleContext)
    {
        MuleMessageCollection coll = new DefaultMessageCollection(muleContext);
        coll.addMessage(event.getMessage());
        event.setMessage(coll);
        return OptimizedRequestContext.unsafeSetEvent(event);
    }

    private MuleEvent createMessageCollection(final List<MuleEvent> nonNullResults,
                                              final MuleEvent previous,
                                              MuleContext muleContext)
    {
        MuleMessageCollection coll = new DefaultMessageCollection(muleContext);
        for (MuleEvent event : nonNullResults)
        {
            coll.addMessage(event.getMessage());
        }
        coll.propagateRootId(previous.getMessage());
        MuleEvent resultEvent = new DefaultMuleEvent(coll, previous, previous.getSession());
        for (String name : previous.getFlowVariableNames())
        {
            resultEvent.setFlowVariable(name, previous.getFlowVariable(name), previous.getFlowVariableDataType(name));
        }
        return OptimizedRequestContext.unsafeSetEvent(resultEvent);
    }
}
