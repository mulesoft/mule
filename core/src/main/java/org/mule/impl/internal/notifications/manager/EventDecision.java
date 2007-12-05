/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications.manager;

public class EventDecision
{

    private ServiceNotificationManager manager;
    private Class event;
    private boolean dynamic = false;
    private boolean enabled = false;

    /**
     * Dynamic decision - check with manager before each use
     */
    public EventDecision(ServiceNotificationManager manager, Class event)
    {
        this.manager = manager;
        this.event = event;
        dynamic = true;
    }

    /**
     * Fixed decision
     */
    public EventDecision(boolean enabled)
    {
        this.enabled = true;
    }

    public boolean isEnabled()
    {
        if (dynamic)
        {
            return manager.isEventEnabled(event);
        }
        else
        {
            return enabled;
        }
    }

}
