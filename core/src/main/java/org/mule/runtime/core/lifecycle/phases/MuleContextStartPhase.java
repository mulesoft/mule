/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.lifecycle.phases;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.agent.Agent;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.registry.Registry;
import org.mule.runtime.core.api.routing.OutboundRouter;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.lifecycle.LifecycleObject;
import org.mule.runtime.core.lifecycle.NotificationLifecycleObject;
import org.mule.runtime.core.util.queue.QueueManager;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Start phase for the MuleContext. Calling
 * {@link MuleContext#start()} will initiate this phase via the
 * {@link org.mule.runtime.core.api.lifecycle.LifecycleManager}.
 * <p/>
 * The MuleContextStartPhase defines the lifecycle behaviour when the Mule context is started.  The MuleContext is associated
 * with one or more registries that inherit the lifecycle of the MuleContext.
 * <p/>
 * This phase is responsible for starting objects. Any object that implements {@link org.mule.runtime.core.api.lifecycle.Startable} will
 * have its {@link org.mule.runtime.core.api.lifecycle.Startable#start()} method called.  Objects are initialised in the order based on type:
 * {@link org.mule.runtime.core.api.agent.Agent}, {@link org.mule.runtime.core.api.construct.FlowConstruct}, followed
 * by any other object that implements {@link org.mule.runtime.core.api.lifecycle.Startable}.
 *
 * @see org.mule.runtime.core.api.MuleContext
 * @see org.mule.runtime.core.api.lifecycle.LifecycleManager
 * @see org.mule.runtime.core.api.lifecycle.Startable
 * @since 3.0
 */
public class MuleContextStartPhase extends DefaultLifecyclePhase
{

    public MuleContextStartPhase()
    {
        this(new Class[] {Registry.class, MuleContext.class, MessageSource.class, Component.class, OutboundRouter.class, MuleContext.class});
    }

    public MuleContextStartPhase(Class<?>[] ignoredObjects)
    {
        super(Startable.PHASE_NAME, Startable.class, Stoppable.PHASE_NAME);

        Set<LifecycleObject> startOrderedObjects = new LinkedHashSet<>();
        startOrderedObjects.add(new NotificationLifecycleObject(QueueManager.class));
        startOrderedObjects.add(new NotificationLifecycleObject(ConfigurationProvider.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Config.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Connector.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
        startOrderedObjects.add(new NotificationLifecycleObject(FlowConstruct.class));
        startOrderedObjects.add(new NotificationLifecycleObject(Startable.class));

        setIgnoredObjectTypes(ignoredObjects);
        setOrderedLifecycleObjects(startOrderedObjects);
        registerSupportedPhase(Initialisable.PHASE_NAME);
        //Start/Stop/Start 
        registerSupportedPhase(Stoppable.PHASE_NAME);
    }
}
