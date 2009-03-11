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
import org.mule.context.notification.MuleContextNotification;
import org.mule.lifecycle.DefaultLifecyclePhase;
import org.mule.lifecycle.NotificationLifecycleObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Stop phase for the Management context LifecycleManager. Calling {@link MuleContext#stop()}
 * with initiate this phase via the {@link org.mule.api.lifecycle.LifecycleManager}.
 * This phase controls the order in which objects should be stopped.
 *
 * @see org.mule.api.MuleContext
 * @see org.mule.api.lifecycle.LifecycleManager
 * @see org.mule.api.lifecycle.Stoppable
 */
public class MuleContextStopPhase extends DefaultLifecyclePhase
{
    public MuleContextStopPhase()
    {
        this(new Class[]{Registry.class, MuleContext.class});
    }

    public MuleContextStopPhase(Class[] ignorredObjects)
    {
        super(Stoppable.PHASE_NAME, Stoppable.class, Startable.PHASE_NAME);

        Set stopOrderedObjects = new LinkedHashSet();
        // Stop in the opposite order to start
        stopOrderedObjects.add(new NotificationLifecycleObject(Connector.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Service.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Model.class, MuleContextNotification.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Stoppable.class));

        setIgnoredObjectTypes(ignorredObjects);
        setOrderedLifecycleObjects(stopOrderedObjects);
        registerSupportedPhase(Startable.PHASE_NAME);
        registerSupportedPhase(Initialisable.PHASE_NAME);
    }
}
