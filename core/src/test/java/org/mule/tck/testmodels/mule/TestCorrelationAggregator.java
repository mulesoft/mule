/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectStoreException;
import org.mule.routing.AbstractCorrelationAggregator;
import org.mule.routing.AggregationException;
import org.mule.routing.EventGroup;

/**
 * <code>TestResponseAggregator</code> is a mock response Agrregator object used for
 * testing configuration
 */
public class TestCorrelationAggregator extends AbstractCorrelationAggregator
{
    private String testProperty;

    @Override
    protected MuleEvent aggregateEvents(EventGroup events) throws AggregationException
    {
        try
        {
            return new DefaultMuleEvent(new DefaultMuleMessage("test", events.toMessageCollection()
                .getMuleContext()), events.getMessageCollectionEvent());
        }
        catch (ObjectStoreException e)
        {
            throw new AggregationException(events,null);
        }
    }

    public String getTestProperty()
    {
        return testProperty;
    }

    public void setTestProperty(String testProperty)
    {
        this.testProperty = testProperty;
    }
}
