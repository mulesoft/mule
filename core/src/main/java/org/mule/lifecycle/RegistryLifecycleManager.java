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
import org.mule.api.lifecycle.LifecyclePair;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.registry.Registry;
import org.mule.lifecycle.phases.NotInLifecyclePhase;
import org.mule.util.StringMessageUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RegistryLifecycleManager implements LifecycleManager
{
    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(RegistryLifecycleManager.class);
    protected static final NotInLifecyclePhase notInLifecyclePhase = new NotInLifecyclePhase();
    protected String currentPhase = notInLifecyclePhase.getName();
    protected String executingPhase = null;
    protected Set completedPhases = new LinkedHashSet(6);
    protected Set transitions = new LinkedHashSet(6);
    protected List<LifecyclePair> lifecyclePairs = new ArrayList<LifecyclePair>(3);
    protected LifecycleState state;

    //this is an internal list to track indexes
    private List<LifecyclePhase> index;

    public RegistryLifecycleManager()
    {
        state = new DefaultLifecycleState(this);
    }

    public List<LifecyclePair> getLifecyclePairs()
    {
        return lifecyclePairs;
    }

    public void setLifecyclePairs(List<LifecyclePair> lifecyclePairs)
    {
        this.lifecyclePairs = lifecyclePairs;
    }

    public void registerLifecycle(LifecyclePair lifecyclePair)
    {
        lifecyclePairs.add(lifecyclePair);
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
            muleContext.getRegistry().fireLifecycle(phase);
            setCurrentPhase(li);
        }
        finally
        {
            setExecutingPhase(null);
        }
    }

    protected List<LifecyclePhase> getPhasesIndex()
    {
        if(index==null)
        {
             index = new ArrayList<LifecyclePhase>(lifecyclePairs.size() * 2 + 1);
            index.add(notInLifecyclePhase);
            for (LifecyclePair pair : lifecyclePairs)
            {
                index.add(pair.getBegin());
            }

            //loop backwards to add the end phases in order
            for (int i = lifecyclePairs.size()-1; i>=0; i--)
            {
               index.add(lifecyclePairs.get(i).getEnd());
            }
        }
        return index;

    }
    protected synchronized int getPhaseIndex(String phase)
    {
        int i = 0;
        for (LifecyclePhase lifecyclePhase : getPhasesIndex())
        {
            if(lifecyclePhase.getName().equals(phase))
            {
                return i;
            }
            i++;
        }
        return -1;
    }

    protected synchronized LifecyclePhase getPhaseForIndex(int phaseIndex)
    {
        return getPhasesIndex().get(phaseIndex);
    }

    protected void doApplyPhase(Registry registry, String phase) throws MuleException
    {
        LifecyclePhase li = lookupPhase(phase);

        if (currentPhase.equalsIgnoreCase(phase))
        {
            logger.debug("Already in lifecycle phase: " + phase);
            return;
        }

        if (!li.isPhaseSupported(currentPhase))
        {
            throw new IllegalStateException("Lifecycle phase: " + phase + " does not support current phase: "
                                            + currentPhase + ". Phases supported are: " + StringMessageUtils.toString(li.getSupportedPhases()));
        }

        try
        {
            setExecutingPhase(phase);
            li.applyLifecycle(registry);
            setCurrentPhase(li);
        }
        finally
        {
            setExecutingPhase(null);
        }
    }



    public void applyPhase(Object object, String phase) throws MuleException
    {
        int current = getPhaseIndex(currentPhase);
        int end = getPhaseIndex(phase);
        if(end < current)
        {
            logger.warn("Phase: " + phase + " has already been fired for this object: " + object);
            return;
        }
        //we want to start at the next one from current
        LifecyclePhase li;
        current++;
        while(current <= end)
        {
            li = getPhaseForIndex(current);
            li.applyLifecycle(object);
            current++;
        }
    }

    public void fireLifecycle(Registry registry, String phase) throws MuleException
    {
        int current = getPhaseIndex(currentPhase);
        int end = getPhaseIndex(phase);
        LifecyclePhase li;

        if(end < current)
        {
            li = getPhaseForIndex(end);
            doApplyPhase(registry, li.getName());
            //logger.warn("Phase: " + phase + " has already been fired for Registry: " + registry.getRegistryId());
            return;
        }

        //we want to start at the next one from current
        current++;
        while(current <= end)
        {
            li = getPhaseForIndex(current);
            doApplyPhase(registry, li.getName());
            current++;
        }
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
        transitions.add(phase.getName());
        transitions.remove(phase.getOppositeLifecyclePhase());
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
        transitions.clear();
        setCurrentPhase(notInLifecyclePhase);
    }

    public boolean isPhaseComplete(String phaseName)
    {
        return transitions.contains(phaseName);
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
            phaseIndex = getPhaseIndex(phase);
            lcp = getPhaseForIndex(phaseIndex);

            if (logger.isDebugEnabled())
            {
                logger.debug("phase: " + lcp);
            }
            lcp.applyLifecycle(object);
        }
        //If we're currently in a phase, fire that too
        if (getExecutingPhase() != null)
        {
            phaseIndex = getPhaseIndex(getExecutingPhase());
            lcp = getPhaseForIndex(phaseIndex);

            if (logger.isDebugEnabled())
            {
                logger.debug("and executing: " + lcp);
            }
            lcp.applyLifecycle(object);
        }
    }

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
            LifecyclePhase phase = getPhaseForIndex(phaseIndex);
            if (!phase.isPhaseSupported(currentPhase))
            {
                throw new IllegalStateException("Lifecycle phase: " + currentPhase + " does not support current phase: "
                        + phase.getName() + ". Phases supported are: " + StringMessageUtils.toString(phase.getSupportedPhases()));
            }
        }
    }

    protected LifecyclePhase lookupPhase(String phase) throws IllegalArgumentException
    {
        Integer phaseIndex = getPhaseIndex(phase);
        if (phaseIndex == null)
        {
            throw new IllegalArgumentException("No lifecycle phase registered with name: " + phase);
        }
        return getPhaseForIndex(phaseIndex);
    }

    public LifecycleState getState()
    {
        return state;
    }
}
