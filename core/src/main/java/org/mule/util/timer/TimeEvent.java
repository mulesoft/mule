/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
