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

import org.mule.umo.manager.UMOServerNotificationListener;

/**
 * <code>ManagementNotificationListener</code> is an observer interface that
 * objects can use to receive notifications about the state of the Mule instance and
 * its resources
 */
public interface ManagementNotificationListener extends UMOServerNotificationListener
{
    // no methods
}
