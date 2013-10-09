/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
            if (event == null)
            {
                return null;
            }
            else if (event instanceof VoidMuleEvent)
            {
                return event;
            }
            else if (event != null && event.getMessage() != null)
            {
                return event;
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
                        return object != null && ((MuleEvent) object).getMessage() != null
                               && !VoidMuleEvent.getInstance().equals(object);
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
            resultEvent.setFlowVariable(name, previous.getFlowVariable(name));
        }
        return OptimizedRequestContext.unsafeSetEvent(resultEvent);
    }
}
