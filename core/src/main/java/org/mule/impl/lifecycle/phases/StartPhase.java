/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.lifecycle.phases;

import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.impl.lifecycle.LifecyclePhase;
import org.mule.impl.lifecycle.NotificationLifecycleObject;
import org.mule.registry.Registry;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * TODO
 */
public class StartPhase extends LifecyclePhase
{
    public StartPhase()
    {
        this(new Class[]{Registry.class, UMOManagementContext.class});
    }

    public StartPhase(Class[] ignorredObjects)
    {
        super(Startable.PHASE_NAME, Startable.class);

        Set startOrderedObjects = new LinkedHashSet();
        startOrderedObjects.add(new NotificationLifecycleObject(UMOConnector.class));
        startOrderedObjects.add(new NotificationLifecycleObject(UMOAgent.class));
        startOrderedObjects.add(new NotificationLifecycleObject(UMOModel.class, ManagerNotification.class,
                ManagerNotification.getActionName(ManagerNotification.MANAGER_STARTING_MODELS),
                ManagerNotification.getActionName(ManagerNotification.MANAGER_STARTED_MODELS)));
        startOrderedObjects.add(new NotificationLifecycleObject(Startable.class));


        setIgnorredObjectTypes(ignorredObjects);
        setOrderedLifecycleObjects(startOrderedObjects);
        registerSupportedPhase(Stoppable.PHASE_NAME);
        registerSupportedPhase(Initialisable.PHASE_NAME);
    }
}