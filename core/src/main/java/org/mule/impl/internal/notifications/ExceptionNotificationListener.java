/*
* $Id$
* --------------------------------------------------------------------------------------
* Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
*
* The software in this package is published under the terms of the MuleSource MPL
* license, a copy of which has been included with this distribution in the
* LICENSE.txt file.
*/

package org.mule.impl.internal.notifications;

import org.mule.umo.manager.UMOServerNotificationListener;

/**
 * <code>ExceptionNotificationListener</code> is an observer interface that
 * objects can implement and then register themselves with the Mule manager to be
 * notified when a Exception event occurs.
 */
public interface ExceptionNotificationListener extends UMOServerNotificationListener
{
    // no methods
}
