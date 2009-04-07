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

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.registry.Registry;
import org.mule.lifecycle.phases.NotInLifecyclePhase;
import org.mule.util.StringMessageUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class GenericLifecycleManager implements LifecycleManager
{
    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(GenericLifecycleManager.class);
    protected static final NotInLifecyclePhase notInLifecyclePhase = new NotInLifecyclePhase();
    protected String currentPhase = notInLifecyclePhase.getName();
    protected String executingPhase = null;
    protected ListOrderedSet lifecycles = new ListOrderedSet();
    protected Map<String, Integer> index = new HashMap<String, Integer>(6);
    protected Set completedPhases = new LinkedHashSet(6);

    public Set getLifecycles()
    {
        return lifecycles;
    }

    public void setLifecycles(Set lifecycles)
    {
        for (Object lifecycle : lifecycles)
        {
            LifecyclePhase phase = (LifecyclePhase) lifecycle;
            registerLifecycle(phase);
        }
    }

    public void registerLifecycle(LifecyclePhase phase)
    {
        index.put(phase.getName(), lifecycles.size());
        lifecycles.add(phase);
    }

    /**
     * Applies lifecycle phase to all objects in the Registry.
     */
    public void firePhase(MuleContext muleContext, String phase) throws MuleException
    {
        if (currentPhase.equalsIgnoreCase(phase))
        {
            logger.debug("Already in lifecycle phase: " + phase);
            return;
        }

        LifecyclePhase li = lookupPhase(phase);
        if (!li.isPhaseSupported(currentPhase))
        {
            throw new IllegalStateException("Lifecycle phase: " + phase + " does not support current phase: "
                                            + currentPhase + ". Phases supported are: " + StringMessageUtils.toString(li.getSupportedPhases()));
        }
        
        try
        {
            setExecutingPhase(phase);
            li.applyLifecycle(muleContext.getRegistry());
            setCurrentPhase(li);
        }
        finally
        {
            setExecutingPhase(null);
        }
    }

    public LifecyclePhase applyPhase(Registry registry, String phase) throws MuleException
    {
        LifecyclePhase li = lookupPhase(phase);
        li.applyLifecycle(registry);
        return li;
    }

    public String getCurrentPhase()
    {
        return currentPhase;
    }

    /**
     * Returns the name of the currently executing phase or null if there is not current phase.
     */
    public String getExecutingPhase()
    {
        return executingPhase;
    }

    protected synchronized void setCurrentPhase(LifecyclePhase phase)
    {
        completedPhases.add(phase.getName());
        completedPhases.remove(phase.getOppositeLifecyclePhase());
        this.currentPhase = phase.getName();
    }

    protected synchronized void setExecutingPhase(String phase)
    {
        this.executingPhase = phase;
    }

    public void reset()
    {
        setExecutingPhase(null);
        completedPhases.clear();
        setCurrentPhase(notInLifecyclePhase);
    }

    public boolean isPhaseComplete(String phaseName)
    {
        return completedPhases.contains(phaseName);
    }

    public void applyCompletedPhases(Object object) throws MuleException
    {
        logger.debug("applying lifecycle to " + object);
        
        LifecyclePhase lcp;
        String phase;
        Integer phaseIndex;
        for (Object completedPhase : completedPhases)
        {
            phase = (String) completedPhase;
            phaseIndex = index.get(phase);
            lcp = (LifecyclePhase) lifecycles.get(phaseIndex);

            if (logger.isDebugEnabled())
            {
                logger.debug("phase: " + lcp);
            }
            lcp.applyLifecycle(object);
        }
        //If we're currently in a phase, fire that too
        if (getExecutingPhase() != null)
        {
            phaseIndex = index.get(getExecutingPhase());
            lcp = (LifecyclePhase) lifecycles.get(phaseIndex);
            
            if (logger.isDebugEnabled())
            {
                logger.debug("and executing: " + lcp);
            }
            lcp.applyLifecycle(object);
        }
    }

    public void checkPhase(String name) throws IllegalStateException
    {
        if (completedPhases.contains(name))
        {
            throw new IllegalStateException("Phase '" + name + "' has already been executed");
        }

        if (name.equalsIgnoreCase(executingPhase))
        {
            throw new IllegalStateException("Phase '" + name + "' is already currently being executed");
        }

        if (executingPhase != null)
        {
            throw new IllegalStateException("Currently executing lifecycle phase: " + executingPhase);
        }

        Integer phaseIndex = index.get(name);
        if (phaseIndex == null)
        {
            throw new IllegalStateException("Phase does not exist: " + name);
        }
        if (NotInLifecyclePhase.PHASE_NAME.equals(currentPhase))
        {
            if (phaseIndex > 0)
            {
                throw new IllegalStateException("The first lifecycle phase has to be called before the '" + name + "' phase");
            }
        }
        else
        {
            LifecyclePhase phase = (LifecyclePhase) lifecycles.get(phaseIndex);
            if (!phase.isPhaseSupported(currentPhase))
            {
                throw new IllegalStateException("Lifecycle phase: " + currentPhase + " does not support current phase: "
                        + name + ". Phases supported are: " + StringMessageUtils.toString(phase.getSupportedPhases()));
            }
        }
    }

    protected LifecyclePhase lookupPhase(String phase) throws IllegalArgumentException
    {
        Integer phaseIndex = index.get(phase);
        if (phaseIndex == null)
        {
            throw new IllegalArgumentException("No lifecycle phase registered with name: " + phase);
        }
        return (LifecyclePhase) lifecycles.get(phaseIndex);
    }
}
