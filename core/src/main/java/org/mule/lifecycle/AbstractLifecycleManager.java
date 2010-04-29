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
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecyclePair;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.ReverseLifecyclePhase;
import org.mule.lifecycle.phases.NotInLifecyclePhase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides a base implementation of a lifecycle manager that will manage all state and transitions
 *
 * Note that {@link org.mule.api.lifecycle.Disposable.PHASE_NAME} can be called from any phase, so if you are customising
 * the lifecycle states you need to handle any transitions before dispose, but invoking those lifecycles on the object being
 * managed.
 */
public abstract class AbstractLifecycleManager implements LifecycleManager, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(AbstractLifecycleManager.class);

    protected static final NotInLifecyclePhase notInLifecyclePhase = new NotInLifecyclePhase();

    protected String currentPhase = notInLifecyclePhase.getName();
    protected String executingPhase = null;
    protected Set<String> completedPhases = new LinkedHashSet<String>(6);
    protected Set<String> transitions = new LinkedHashSet<String>(6);
    protected List<LifecyclePair> lifecyclePairs = new ArrayList<LifecyclePair>(3);
    protected LifecycleState state;
    protected MuleContext muleContext;

    //this is an internal list to track indexes
    private List<LifecyclePhase> index;

    public AbstractLifecycleManager()
    {
        state = createLifecycleState();
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    protected LifecycleState createLifecycleState()
    {
        return new DefaultLifecycleState(this);
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

    protected abstract void doApplyPhase(LifecyclePhase phase) throws LifecycleException;

    protected void invokePhase(LifecyclePhase phase) throws LifecycleException
    {

        try
        {
            setExecutingPhase(phase.getName());
            doApplyPhase(phase);
            setCurrentPhase(phase);
        }
        finally
        {
            setExecutingPhase(null);
        }
    }

    public void applyPhase(Object object, String phase) throws LifecycleException
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

    public void fireLifecycle(String phase) throws LifecycleException
    {
        checkPhase(phase);
        
        int current = getPhaseIndex(currentPhase);
        int end = getPhaseIndex(phase);
        LifecyclePhase li;

        if(end < current)
        {
            li = getPhaseForIndex(end);
            invokePhase(li);
            //logger.warn("Phase: " + phase + " has already been fired for Registry: " + registry.getRegistryId());
            return;
        }

        //we want to start at the next one from current
        current++;
        while(current <= end)
        {
            li = getPhaseForIndex(current);
            invokePhase(li);
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
        if(phase instanceof ReverseLifecyclePhase)
        {
            //don't record the meta phase transition
            transitions.remove(phase.getOppositeLifecyclePhase());
            //Revert back to the phase before this meta lifecycle pair was invoked
            this.currentPhase = completedPhases.toArray(new String[]{})[completedPhases.size()-2];
        }
        else
        {
            completedPhases.add(phase.getName());
            transitions.add(phase.getName());
            transitions.remove(phase.getOppositeLifecyclePhase());
            this.currentPhase = phase.getName();
        }
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

    public void applyCompletedPhases(Object object) throws LifecycleException
    {
        
        logger.debug("applying lifecycle to " + object);

        LifecyclePhase lcp;
        String phase;
        int phaseIndex;
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
            if(phase.equals(getCurrentPhase()))
            {
                break;
            }
        }
        //If we're currently in a phase, fire that too
        //TODO we can trip ourselves up here if an ordered lf object gets started before it should
        //TODO we may need to cache objects that are added during a phase, then fire lifecycle on those objects
        //once the phase is about to finish
        //At least now that lifecycle is locked down we will not get an inconsistent state without knowing about it.
//        if (getExecutingPhase() != null)
//        {
//            phaseIndex = getPhaseIndex(getExecutingPhase());
//            lcp = getPhaseForIndex(phaseIndex);
//
//            if (logger.isDebugEnabled())
//            {
//                logger.debug("and executing: " + lcp);
//            }
//            lcp.applyLifecycle(object);
//        }
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


        int phaseIndex = getPhaseIndex(name);
        if (phaseIndex == -1)
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
            throw new IllegalStateException("Lifecycle phase: " + currentPhase + " does not support phase: " + name);
        }
    }

    protected LifecyclePhase lookupPhase(String phase) throws IllegalArgumentException
    {
        int phaseIndex = getPhaseIndex(phase);
        if (phaseIndex == -1)
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
