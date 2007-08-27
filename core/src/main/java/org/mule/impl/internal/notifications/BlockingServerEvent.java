/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

/**
 * <code>BlockingServerEvent</code> is a marker interface that tells the server
 * event manager to publish this event in the current thread, thus blocking the
 * current thread of execution until all listeners have been processed
 */

public interface BlockingServerEvent
{
    // no methods
}
