/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
