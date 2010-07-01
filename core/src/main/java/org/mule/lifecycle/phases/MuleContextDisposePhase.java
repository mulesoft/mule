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

import org.mule.api.agent.Agent;
import org.mule.api.component.Component;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.model.Model;
import org.mule.api.routing.Router;
import org.mule.api.routing.RouterCollection;
import org.mule.api.service.Service;
import org.mule.api.source.MessageSource;
import org.mule.api.transport.Connector;
import org.mule.context.notification.MuleContextNotification;
import org.mule.lifecycle.DefaultLifecyclePhase;
import org.mule.lifecycle.LifecycleObject;
import org.mule.lifecycle.NotificationLifecycleObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Objects are disposed of via the Registry since the Registry manages the creation/initialisation of the objects
 * it must also take care of disposing them. However, a user may want to initiate a dispose via the
 * {@link org.mule.DefaultMuleContext} so the dispose Lifecycle phase for the {@link org.mule.DefaultMuleContext}
 * needs to call dispose on the Registry.
 *
 * The MuleContextDisposePhase defines the lifecycle behaviour when the Mule context is disposed.  The MuleContext is associated
 * with one or more registries that inherit the lifecycle of the MuleContext.
 *
 * This phase is responsible for disposing objects. Any object that implements {@link org.mule.api.lifecycle.Disposable} will
 * have its {@link org.mule.api.lifecycle.Disposable#dispose()} method called.  Objects are initialised in the order based on type:
 * {@link org.mule.api.service.Service}, {@link org.mule.api.model.Model}, {@link org.mule.api.agent.Agent}, {@link org.mule.api.transport.Connector}, followed
 * by any other object that implements {@link org.mule.api.lifecycle.Disposable}.
 *
 * @see org.mule.api.MuleContext
 * @see org.mule.api.lifecycle.LifecycleManager
 * @see org.mule.api.lifecycle.Disposable
 *
 * @since 3.0
 */
public class MuleContextDisposePhase extends DefaultLifecyclePhase
{
    public MuleContextDisposePhase()
    {
        super(Disposable.PHASE_NAME, Disposable.class, Initialisable.PHASE_NAME);

        Set<LifecycleObject> stopOrderedObjects = new LinkedHashSet<LifecycleObject>();
        // Stop in the opposite order to start
        stopOrderedObjects.add(new NotificationLifecycleObject(Service.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Model.class, MuleContextNotification.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Connector.class));
        stopOrderedObjects.add(new NotificationLifecycleObject(Stoppable.class));

        //Can call dispose from all lifecycle Phases
        registerSupportedPhase(LifecyclePhase.ALL_PHASES);
        setOrderedLifecycleObjects(stopOrderedObjects);
        setIgnoredObjectTypes(new Class[]{Component.class, MessageSource.class, RouterCollection.class, Router.class});
    }
}
