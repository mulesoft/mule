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

import org.mule.impl.lifecycle.LifecyclePhase;
import org.mule.impl.lifecycle.NotificationLifecycleObject;
import org.mule.registry.Registry;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Initialise phase for the TransientRegistry LifecycleManager. Calling {@link org.mule.impl.registry.TransientRegistry#initialise()}
 * with initiate this phase via the {@link org.mule.umo.lifecycle.UMOLifecycleManager}.
 * This phase controls the order in which objects should be initialised.
 *
 * @see org.mule.umo.UMOManagementContext
 * @see org.mule.umo.lifecycle.UMOLifecycleManager
 * @see org.mule.impl.registry.TransientRegistry
 * @see org.mule.umo.lifecycle.Initialisable
 */
public class TransientRegistryInitialisePhase extends LifecyclePhase
{
    public TransientRegistryInitialisePhase()
    {
        this(new Class[]{Registry.class});
    }

    public TransientRegistryInitialisePhase(Class[] ignorredObjects)
    {
        super(Initialisable.PHASE_NAME, Initialisable.class, Disposable.PHASE_NAME);

        setIgnorredObjectTypes(ignorredObjects);
        Set initOrderedObjects = new LinkedHashSet();
        initOrderedObjects.add(new NotificationLifecycleObject(UMOManagementContext.class));
        initOrderedObjects.add(new NotificationLifecycleObject(UMOConnector.class));
        initOrderedObjects.add(new NotificationLifecycleObject(UMOTransformer.class));
        initOrderedObjects.add(new NotificationLifecycleObject(UMOImmutableEndpoint.class));
        initOrderedObjects.add(new NotificationLifecycleObject(UMOAgent.class));
        initOrderedObjects.add(new NotificationLifecycleObject(UMODescriptor.class));
        initOrderedObjects.add(new NotificationLifecycleObject(UMOModel.class));

        initOrderedObjects.add(new NotificationLifecycleObject(Initialisable.class));

        setOrderedLifecycleObjects(initOrderedObjects);
        registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);
    }
}
