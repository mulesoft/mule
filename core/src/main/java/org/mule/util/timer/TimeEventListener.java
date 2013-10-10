/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.timer;

import java.util.EventListener;

/**
 * <code>TimeEventListener</code> provides a method to pass timer events to
 * implementing objects.
 */
public interface TimeEventListener extends EventListener
{
    /**
     * Passes the TimeEvent to an object
     * 
     * @param e the time event that occurred
     */
    void timeExpired(TimeEvent e);
}
