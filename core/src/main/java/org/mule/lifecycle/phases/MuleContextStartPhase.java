/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle.phases;

import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.model.Model;
import org.mule.api.registry.Registry;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.context.notification.ManagerNotification;
import org.mule.lifecycle.DefaultLifecyclePhase;
import org.mule.lifecycle.NotificationLifecycleObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Start phase for the Management context LifecycleManager. Calling {@link org.mule.api.UMOManagementContext#start()}
 * with initiate this phase via the {@link org.mule.api.lifecycle.LifecycleManager}.
 * This phase controls the order in which objects should be started.
 *
 * @see org.mule.api.UMOManagementContext
 * @see org.mule.api.lifecycle.LifecycleManager
 * @see org.mule.api.lifecycle.Startable
 */
public class MuleContextStartPhase extends DefaultLifecyclePhase
{
    public MuleContextStartPhase()
    {
        this(new Class[]{Registry.class, MuleContext.class});
    }

    public MuleContextStartPhase(Class[] ignorredObjects)
    {
        super(Startable.PHASE_NAME, Startable.class, Stoppable.PHASE_NAME);

        Set startOrderedObjects = new LinkedHashSet();
        startOrderedObjects.add(new NotificationLifecycleObject(Connector.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Model.class, ManagerNotification.class,
                ManagerNotification.MANAGER_STARTING_MODELS,ManagerNotification.MANAGER_STARTED_MODELS));
        startOrderedObjects.add(new NotificationLifecycleObject(Service.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Startable.class));


        setIgnorredObjectTypes(ignorredObjects);
        setOrderedLifecycleObjects(startOrderedObjects);
        registerSupportedPhase(Stoppable.PHASE_NAME);
        registerSupportedPhase(Initialisable.PHASE_NAME);
    }
}