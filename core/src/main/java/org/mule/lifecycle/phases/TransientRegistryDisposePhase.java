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
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.model.Model;
import org.mule.api.registry.Registry;
import org.mule.api.transport.Connector;
import org.mule.context.notification.ManagerNotification;
import org.mule.lifecycle.DefaultLifecyclePhase;
import org.mule.lifecycle.NotificationLifecycleObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Dispose phase for the TransientRegistry LifecycleManager. Calling {@link org.mule.registry.TransientRegistry#dispose()}
 * with initiate this phase via the {@link org.mule.api.lifecycle.LifecycleManager}.
 * This phase controls the order in which objects should be disposed.
 *
 * @see org.mule.api.MuleContext
 * @see org.mule.api.lifecycle.LifecycleManager
 * @see org.mule.registry.TransientRegistry
 * @see org.mule.api.lifecycle.Disposable
 */
public class TransientRegistryDisposePhase extends DefaultLifecyclePhase
{
    public TransientRegistryDisposePhase()
    {
        this(new Class[]{Registry.class});
    }

    public TransientRegistryDisposePhase(Class[] ignorredObjects)
    {
        super(Disposable.PHASE_NAME, Disposable.class, Initialisable.PHASE_NAME);

        Set disposeOrderedObjects = new LinkedHashSet();
//        disposeOrderedObjects.add(new NotificationLifecycleObject(MuleContext.class, ManagerNotification.class,
//                ManagerNotification.getActionName(ManagerNotification.MANAGER_DISPOSING),
//                ManagerNotification.getActionName(ManagerNotification.MANAGER_DISPOSED)));
        disposeOrderedObjects.add(new NotificationLifecycleObject(MuleContext.class));
        try
        {
            disposeOrderedObjects.add(new NotificationLifecycleObject(Connector.class, ManagerNotification.class,
                    ManagerNotification.MANAGER_DISPOSING_CONNECTORS,ManagerNotification.MANAGER_DISPOSED_CONNECTORS));
        }
        catch (IllegalStateException e)
        {
            // TODO MULE-2903 Fix again for OSGi: The hack for MULE-2903 (in NotificationLifecycleObject()) 
            // throws an IllegalStateException when calling ClassUtils.initializeClass()            
        }
        disposeOrderedObjects.add(new NotificationLifecycleObject(Agent.class));
        disposeOrderedObjects.add(new NotificationLifecycleObject(Model.class));
        disposeOrderedObjects.add(new NotificationLifecycleObject(Disposable.class));

        setIgnoredObjectTypes(ignorredObjects);
        setOrderedLifecycleObjects(disposeOrderedObjects);
        registerSupportedPhase(LifecyclePhase.ALL_PHASES);
    }
}
