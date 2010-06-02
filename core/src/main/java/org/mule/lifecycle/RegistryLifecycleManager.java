/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePair;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.registry.Registry;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RegistryLifecycleManager extends AbstractLifecycleManager
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(RegistryLifecycleManager.class);

    private Registry registry;

    public RegistryLifecycleManager(Registry registry)
    {
        this.registry = registry;
    }

    @Override
    protected void doApplyPhase(LifecyclePhase phase) throws LifecycleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Applying lifecycle phase: " + phase.getName() + " for registry: " + registry.getClass().getSimpleName());
        }

        if(phase instanceof ContainerManagedLifecyclePhase)
        {
            phase.applyLifecycle(registry);
            return;
        }

        // overlapping interfaces can cause duplicates
        Set<Object> duplicates = new HashSet<Object>();

        for (LifecycleObject lo : phase.getOrderedLifecycleObjects())
        {
            // TODO Collection -> List API refactoring
            Collection<?> targetsObj = registry.lookupObjects(lo.getType());
            List targets = new LinkedList(targetsObj);
            if (targets.size() == 0)
            {
                continue;
            }

            lo.firePreNotification(muleContext);

            for (Iterator target = targets.iterator(); target.hasNext();)
            {
                Object o = target.next();
                if (duplicates.contains(o))
                {
                    target.remove();
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("lifecycle phase: " + phase.getName() + " for object: " + o);
                    }
                    phase.applyLifecycle(o);
                    target.remove();
                    duplicates.add(o);
                }
            }

            lo.firePostNotification(muleContext);
        }
    }

    @Override
    public void checkPhase(String name) throws IllegalStateException
    {
        if (executingPhase != null)
        {
            if (name.equalsIgnoreCase(executingPhase))
            {
                throw new IllegalStateException("Phase '" + name + "' is already currently being executed");
            }
            else
            {
                throw new IllegalStateException("Currently executing lifecycle phase: " + executingPhase);
            }
        }

        if (name.equalsIgnoreCase(currentPhase))
        {
            throw new IllegalStateException("Already in lifecycle phase '" + name + "', cannot fire the same phase twice");
        }


        Integer phaseIndex = getPhaseIndex(name);
        if (phaseIndex == null)
        {
            throw new IllegalStateException("Phase does not exist: " + name);
        }
        else
        {
            //Allow dispose to be called from any other lifecycle
            if(Disposable.PHASE_NAME.equals(name))
            {
                return;
            }
            //We can always transition to the next phase
            if(index.get(phaseIndex-1).getName().equals(getCurrentPhase()))
            {
                return;
            }
            for (LifecyclePair pair : lifecyclePairs)
            {
                //Always allow a phase to transition from begin phase to end phase
                if(pair.getBegin().getName().equals(name) && pair.getEnd().getName().equals(getCurrentPhase()) ||
                   pair.getEnd().getName().equals(name) && pair.getBegin().getName().equals(getCurrentPhase()))
                {
                    return;
                }
            }
            throw new IllegalStateException("Registry " + registry.getRegistryId() + " Lifecycle phase: " + currentPhase + " does not support phase: " + name);
        }
    }
}
