/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.registry.Registry;
import org.mule.lifecycle.phases.ContainerManagedLifecyclePhase;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
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
        Set<Object> duplicates = new HashSet<>();

        for (LifecycleObject lifecycleObject : phase.getOrderedLifecycleObjects())
        {
            lifecycleObject.firePreNotification(registryLifecycleManager.muleContext);

            // TODO Collection -> List API refactoring
            Collection<?> targetsObj = lookupObjectsForLifecycle(lifecycleObject);
            doApplyLifecycle(phase, duplicates, lifecycleObject, targetsObj);
            lifecycleObject.firePostNotification(registryLifecycleManager.muleContext);
        }
    }

    private void doApplyLifecycle(LifecyclePhase phase, Set<Object> duplicates, LifecycleObject lifecycleObject, Collection<?> targetObjects) throws LifecycleException
    {
        if (CollectionUtils.isEmpty(targetObjects))
        {
            return;
        }

        for (Object target : targetObjects)
        {
            if (duplicates.contains(target))
            {
                continue;
            }

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("lifecycle phase: " + phase.getName() + " for object: " + target);
            }

            phase.applyLifecycle(target);
            duplicates.add(target);
        }

        // the target object might have created and registered a new object
        // (e.g.: an endpoint which registers a connector)
        // check if there're new objects for the phase
        int originalTargetCount = targetObjects.size();
        targetObjects = lookupObjectsForLifecycle(lifecycleObject);
        if (targetObjects.size() > originalTargetCount)
        {
            doApplyLifecycle(phase, duplicates, lifecycleObject, targetObjects);
        }
    }

    protected Collection<?> lookupObjectsForLifecycle(LifecycleObject lo)
    {
        return registryLifecycleManager.getLifecycleObject().lookupObjectsForLifecycle(lo.getType());
    }
}
