/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
