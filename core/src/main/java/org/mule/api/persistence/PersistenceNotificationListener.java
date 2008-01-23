/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.persistence;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;

/**
 * 
 * @author 
 * @version $Revision$
 */
public class PersistenceNotificationListener implements ServerNotificationListener
{
    private PersistenceManager persistenceManager;

    public PersistenceNotificationListener(PersistenceManager persistenceManager)
    {
        this.persistenceManager = persistenceManager;
    }

    public void onNotification(ServerNotification notification)
    {
        if (notification instanceof PersistenceNotification)
        {
            Persistable object = (Persistable)notification.getSource();
            persistenceManager.requestPersistence(object);
        }

    }
}

