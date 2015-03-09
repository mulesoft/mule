/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle.phases;

import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.api.component.Component;
import org.mule.api.config.Config;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.model.Model;
import org.mule.api.registry.Registry;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.source.MessageSource;
import org.mule.api.transport.Connector;
import org.mule.lifecycle.LifecycleObject;
import org.mule.lifecycle.NotificationLifecycleObject;
import org.mule.util.queue.QueueManager;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Start phase for the MuleContext. Calling
 * {@link MuleContext#start()} will initiate this phase via the
 * {@link org.mule.api.lifecycle.LifecycleManager}.
 *
 * The MuleContextStartPhase defines the lifecycle behaviour when the Mule context is started.  The MuleContext is associated
 * with one or more registries that inherit the lifecycle of the MuleContext.
 *
 * This phase is responsible for starting objects. Any object that implements {@link org.mule.api.lifecycle.Startable} will
 * have its {@link org.mule.api.lifecycle.Startable#start()} method called.  Objects are initialised in the order based on type:
 * {@link org.mule.api.transport.Connector}, {@link org.mule.api.agent.Agent}, {@link org.mule.api.model.Model}, {@link org.mule.api.service.Service}, followed
 * by any other object that implements {@link org.mule.api.lifecycle.Startable}.
 *
 * @see org.mule.api.MuleContext
 * @see org.mule.api.lifecycle.LifecycleManager
 * @see org.mule.api.lifecycle.Startable
 *
 * @since 3.0
 */
public class MuleContextStartPhase extends DefaultLifecyclePhase
{
    public MuleContextStartPhase()
    {
        this(new Class[]{Registry.class, MuleContext.class, MessageSource.class, Component.class, OutboundRouterCollection.class, OutboundRouter.class});
    }

    public MuleContextStartPhase(Class<?>[] ignorredObjects)
    {
        super(Startable.PHASE_NAME, Startable.class, Stoppable.PHASE_NAME);

        Set<LifecycleObject> startOrderedObjects = new LinkedHashSet<LifecycleObject>();
        startOrderedObjects.add(new NotificationLifecycleObject(QueueManager.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Config.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Connector.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Model.class));
        startOrderedObjects.add(new NotificationLifecycleObject(FlowConstruct.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Startable.class));

        setIgnoredObjectTypes(ignorredObjects);
        setOrderedLifecycleObjects(startOrderedObjects);
        registerSupportedPhase(Initialisable.PHASE_NAME);
        //Start/Stop/Start 
        registerSupportedPhase(Stoppable.PHASE_NAME);
    }
}
