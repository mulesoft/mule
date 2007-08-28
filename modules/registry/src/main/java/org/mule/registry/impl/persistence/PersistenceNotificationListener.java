/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl.persistence;

import org.mule.persistence.Persistable;
import org.mule.registry.impl.store.PersistenceManager;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;

/**
 * 
 * @author 
 * @version $Revision: $
 */
public class PersistenceNotificationListener implements UMOServerNotificationListener
{
    private PersistenceManager persistenceManager;

    public PersistenceNotificationListener(PersistenceManager persistenceManager)
    {
        this.persistenceManager = persistenceManager;
    }

    public void onNotification(UMOServerNotification notification)
    {
        System.out.println("Got notification");
        if (notification instanceof PersistenceNotification)
        {
            Persistable object = (Persistable)notification.getSource();
            //persistenceManager.requestPersistence(object);
        }

    }
}

