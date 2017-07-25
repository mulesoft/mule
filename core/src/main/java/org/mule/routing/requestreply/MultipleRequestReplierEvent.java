/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.requestreply;

import org.mule.api.MuleEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MultipleRequestReplierEvent implements Serializable
{
    private final List<MuleEvent> muleEvents = new ArrayList<>();

    protected synchronized void addEvent(MuleEvent event)
    {
        muleEvents.add(event);
    }

    protected synchronized void removeEvent()
    {
        muleEvents.remove(0);
    }

    protected synchronized MuleEvent getEvent()
    {
        return muleEvents.get(0);
    }
}
