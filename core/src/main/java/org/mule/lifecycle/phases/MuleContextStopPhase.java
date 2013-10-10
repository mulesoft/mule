/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle.phases;

import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.api.component.Component;
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
import org.mule.context.notification.MuleContextNotification;
import org.mule.lifecycle.LifecycleObject;
import org.mule.lifecycle.NotificationLifecycleObject;
import org.mule.util.queue.TransactionalQueueManager;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Stop phase for the Management context LifecycleManager. Calling {@link MuleContext#stop()}
 * with initiate this phase via the {@link org.mule.api.lifecycle.LifecycleManager}.
 *
 *
 * The MuleContextDisposePhase defines the lifecycle behaviour when the Mule context is stopped.  The MuleContext is associated
 * with one or more registries that inherit the lifecycle of the MuleContext.
 *
 * This phase is responsible for disposing objects. Any object that implements {@link org.mule.api.lifecycle.Stoppable} will
 * have its {@link org.mule.api.lifecycle.Stoppable#stop()} ()} method called.  Objects are initialised in the order based on type:
 * {@link org.mule.api.service.Service}, {@link org.mule.api.model.Model}, {@link org.mule.api.agent.Agent}, {@link org.mule.api.transport.Connector}, followed
 * by any other object that implements {@link org.mule.api.lifecycle.Stoppable}.
 *
 * @see org.mule.api.MuleContext
 * @see org.mule.api.lifecycle.LifecycleManager
 * @see org.mule.api.lifecycle.Stoppable
 *
 * @since 3.0
 */
public class MuleContextStopPhase extends DefaultLifecyclePhase
{
    public MuleContextStopPhase()
    {
        this(new Class[]{Registry.class, MuleContext.class, MessageSource.class, Component.class, OutboundRouterCollection.class, OutboundRouter.class});
    }

    public MuleContextStopPhase(Class<?>[] ignorredObjects)
    {
        super(Stoppable.PHASE_NAME, Stoppable.class, Startable.PHASE_NAME);

        Set<LifecycleObject> stopOrderedObjects = new LinkedHashSet<LifecycleObject>();
        // Stop in the opposite order to start
        stopOrderedObjects.add(new NotificationLifecycleObject(FlowConstruct.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Model.class, MuleContextNotification.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Connector.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(TransactionalQueueManager.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Stoppable.class));

        setIgnoredObjectTypes(ignorredObjects);
        setOrderedLifecycleObjects(stopOrderedObjects);
        //Yuo can initialise and stop
        registerSupportedPhase(Initialisable.PHASE_NAME);
        //Stop/Start/Stop
        registerSupportedPhase(Startable.PHASE_NAME);
    }
}
