/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.impl;

import org.mule.impl.internal.notifications.ModelNotification;
import org.mule.registry.Registry;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.model.UMOModel;

/**
 */
public class RegistryNotificationListener implements UMOServerNotificationListener
{
    private Registry registry;

    public RegistryNotificationListener(Registry registry)
    {
        this.registry = registry;
    }

    public void onNotification(UMOServerNotification notification)
    {
        //System.out.println("Received event " + notification.getClass().getName());
        //System.out.println("Source is " + notification.getSource().getClass().getName());

        if (notification instanceof ModelNotification)
        {
            UMOModel model = (UMOModel)notification.getSource();
            //System.out.println("Model name: " + model.getName());
            //System.out.println("Model type: " + model.getType());
            registry.notifyStateChange(model.getRegistryId(), notification.getAction());
        }

    }
}

