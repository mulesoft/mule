/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.Serializable;

/**
 * Wraps the {@link MuleEvent} and saves the timeOrder of this to an {@link EventGroup}.
 */
class WrapperOrderEvent implements Serializable, DeserializationPostInitialisable
{

    final MuleEvent event;
    final long timeOrder;

    public WrapperOrderEvent(MuleEvent event, long timeOrder)
    {
        this.event = event;
        this.timeOrder= timeOrder;
    }

    public MuleEvent getEvent()
    {
        return event;
    }

    public long getTimeOrder()
    {
        return timeOrder;
    }

    @SuppressWarnings({"unused"})
    private void initAfterDeserialisation(MuleContext muleContext) throws Exception
    {
        DeserializationPostInitialisable.Implementation.init(event, muleContext);
    }

}