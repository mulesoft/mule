/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.timer;

import java.util.EventObject;

/**
 * <code>TimeEvent</code> TODO is an event that occurs at a specified number of
 * milliseconds.
 */
public class TimeEvent extends EventObject
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7540426406525372393L;

    /**
     * The event name
     */
    private String name;

    private long timeExpired;

    public TimeEvent(Object source, String name, long timeExpired)
    {
        super(source);
        this.name = name;
        this.timeExpired = timeExpired;
    }

    public String getName()
    {
        return name;
    }

    public long getTimeExpired()
    {
        return timeExpired;
    }
}
