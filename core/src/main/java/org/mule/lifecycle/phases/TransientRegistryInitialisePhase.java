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
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.model.Model;
import org.mule.api.registry.Registry;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.lifecycle.DefaultLifecyclePhase;
import org.mule.lifecycle.NotificationLifecycleObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Initialise phase for the TransientRegistry LifecycleManager. Calling {@link org.mule.registry.TransientRegistry#initialise()}
 * with initiate this phase via the {@link org.mule.api.lifecycle.LifecycleManager}.
 * This phase controls the order in which objects should be initialised.
 *
 * @see org.mule.api.MuleContext
 * @see org.mule.api.lifecycle.LifecycleManager
 * @see org.mule.registry.TransientRegistry
 * @see org.mule.api.lifecycle.Initialisable
 */
public class TransientRegistryInitialisePhase extends DefaultLifecyclePhase
{
    public TransientRegistryInitialisePhase()
    {
        this(new Class[]{Registry.class});
    }

    public TransientRegistryInitialisePhase(Class[] ignorredObjects)
    {
        super(Initialisable.PHASE_NAME, Initialisable.class, Disposable.PHASE_NAME);

        setIgnoredObjectTypes(ignorredObjects);
        Set initOrderedObjects = new LinkedHashSet();
        initOrderedObjects.add(new NotificationLifecycleObject(MuleContext.class));
        initOrderedObjects.add(new NotificationLifecycleObject(Connector.class));
        initOrderedObjects.add(new NotificationLifecycleObject(Transformer.class));
        initOrderedObjects.add(new NotificationLifecycleObject(ImmutableEndpoint.class));
        initOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
        initOrderedObjects.add(new NotificationLifecycleObject(Service.class));
        initOrderedObjects.add(new NotificationLifecycleObject(Model.class));

        initOrderedObjects.add(new NotificationLifecycleObject(Initialisable.class));

        setOrderedLifecycleObjects(initOrderedObjects);
        registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);
    }
}
