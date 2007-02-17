/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

import org.mule.umo.manager.UMOServerNotification;

/**
 * Notifications to be sent on each lifecycle event.  Null should be returned if no notification 
 * is to be sent.
 */
public interface LifecycleNotifications
{
    UMOServerNotification getNotificationInitialising();
    
    UMOServerNotification getNotificationInitialised();
    
    UMOServerNotification getNotificationDisposing();
    
    UMOServerNotification getNotificationDisposed();
    
    UMOServerNotification getNotificationStarting();
    
    UMOServerNotification getNotificationStarted();
    
    UMOServerNotification getNotificationStopping();
    
    UMOServerNotification getNotificationStopped();

    UMOServerNotification getNotificationPaused();
    
    UMOServerNotification getNotificationResumed();    
}


