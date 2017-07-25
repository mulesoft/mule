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
