/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
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
 * The Start phase for the Management context LifecycleManager. Calling {@link org.mule.umo.UMOManagementContext#start()}
 * with initiate this phase via the {@link org.mule.umo.lifecycle.UMOLifecycleManager}.
 * This phase controls the order in which objects should be started.
 *
 * @see org.mule.umo.UMOManagementContext
 * @see org.mule.umo.lifecycle.UMOLifecycleManager
 * @see org.mule.umo.lifecycle.Startable
 */
public class ManagementContextStartPhase extends LifecyclePhase
{
    public ManagementContextStartPhase()
    {
        this(new Class[]{Registry.class, UMOManagementContext.class});
    }

    public ManagementContextStartPhase(Class[] ignorredObjects)
    {
        super(Startable.PHASE_NAME, Startable.class, Stoppable.PHASE_NAME);

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