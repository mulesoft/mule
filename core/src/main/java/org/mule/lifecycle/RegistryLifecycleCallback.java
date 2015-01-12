/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.registry.Registry;
import org.mule.lifecycle.phases.ContainerManagedLifecyclePhase;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link LifecycleCallback} for applying {@link Registry} lifecycles
 *
 * @since 3.7.0
 */
public class RegistryLifecycleCallback<T> implements LifecycleCallback<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryLifecycleCallback.class);

    protected final RegistryLifecycleManager registryLifecycleManager;

    public RegistryLifecycleCallback(RegistryLifecycleManager registryLifecycleManager)
    {
        this.registryLifecycleManager = registryLifecycleManager;
    }

    @Override
    public void onTransition(String phaseName, T object) throws MuleException
    {
        LifecyclePhase phase = registryLifecycleManager.phases.get(phaseName);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(String.format("Applying lifecycle phase: %s for registry: %s", phase, object.getClass().getSimpleName()));
        }

        if (phase instanceof ContainerManagedLifecyclePhase)
        {
            phase.applyLifecycle(object);
            return;
        }

        // overlapping interfaces can cause duplicates
        // TODO: each LifecycleManager should keep this set per executing phase
        // and clear it when the phase is fully applied
        Set<Object> duplicates = new HashSet<Object>();

        for (LifecycleObject lifecycleObject : phase.getOrderedLifecycleObjects())
        {
            // TODO Collection -> List API refactoring
            Collection<?> targetsObj = lookupObjectsForLifecycle(lifecycleObject);
            if (targetsObj.size() == 0)
            {
                continue;
            }

            List<Object> targets = new LinkedList<>(targetsObj);
            lifecycleObject.firePreNotification(registryLifecycleManager.muleContext);

            for (Iterator<Object> target = targets.iterator(); target.hasNext();)
            {
                Object o = target.next();
                if (duplicates.contains(o))
                {
                    target.remove();
                }
                else
                {
                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug("lifecycle phase: " + phase.getName() + " for object: " + o);
                    }
                    phase.applyLifecycle(o);
                    target.remove();
                    duplicates.add(o);
                }
            }

            lifecycleObject.firePostNotification(registryLifecycleManager.muleContext);
        }
    }

    protected Collection<?> lookupObjectsForLifecycle(LifecycleObject lo)
    {
        return registryLifecycleManager.getLifecycleObject().lookupObjectsForLifecycle(lo.getType());
    }
}
