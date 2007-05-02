/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.lifecycle.phases;

import org.mule.config.spring.ObjectProcessor;
import org.mule.config.spring.RegistryFacade;
import org.mule.impl.internal.notifications.RegistryNotification;
import org.mule.impl.lifecycle.LifecyclePhase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.UMOLifecyclePhase;

import java.util.Iterator;
import java.util.Map;

/**
 * TODO
 */
public class RegistryInitialisePhase extends LifecyclePhase
{
    public RegistryInitialisePhase()
    {
        super(Initialisable.PHASE_NAME, Initialisable.class);
        registerSupportedPhase(UMOLifecyclePhase.NOT_IN_LIFECYCLE_PHASE);
    }

    //@java.lang.Override
    public void fireLifecycle(UMOManagementContext managementContext, String currentPhase) throws UMOException
    {
        RegistryFacade registry = managementContext.getRegistry();
        int oldScope = registry.getDefaultScope();
        registry.setDefaultScope(RegistryFacade.SCOPE_IMMEDIATE);
        try
        {
            applyProcessors(registry, registry.getConnectors());
            applyProcessors(registry, registry.getTransformers());
            applyProcessors(registry, registry.getEndpoints());
            applyProcessors(registry, registry.getAgents());
            applyProcessors(registry, registry.getModels());
            applyProcessors(registry, registry.getServices());
            applyProcessors(registry, registry.lookupCollection(Object.class));

            managementContext.fireNotification(new RegistryNotification(managementContext.getRegistry(), RegistryNotification.REGISTRY_INITIALISED));
        }
        finally
        {
            registry.setDefaultScope(oldScope);
        }

    }

    protected void applyProcessors(RegistryFacade registry, Map objects)
    {
        if(objects==null) return;
        for (Iterator iterator = objects.values().iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            appyProcessors(registry, o);
        }
    }

    protected void appyProcessors(RegistryFacade registry, Object object)
    {
        Map processors = registry.lookupCollection(ObjectProcessor.class);
        for (Iterator iterator = processors.values().iterator(); iterator.hasNext();)
        {
            ObjectProcessor o = (ObjectProcessor)iterator.next();
            o.process(object);
        }
    }

}