/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.RegistryLifecycleHelpers;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.Registry;
import org.mule.config.i18n.CoreMessages;
import org.mule.lifecycle.phases.ContainerManagedLifecyclePhase;
import org.mule.lifecycle.phases.MuleContextDisposePhase;
import org.mule.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.lifecycle.phases.NotInLifecyclePhase;
import org.mule.registry.AbstractRegistryBroker;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RegistryLifecycleManager extends AbstractLifecycleManager<Registry> implements RegistryLifecycleHelpers
{
    protected Map<String, LifecyclePhase> phases = new HashMap<String, LifecyclePhase>();
    protected TreeMap<String, LifecycleCallback> callbacks = new TreeMap<String, LifecycleCallback>();
    protected MuleContext muleContext;

    public RegistryLifecycleManager(String id, Registry object, MuleContext muleContext)
    {
        super(id, object);
        this.muleContext = muleContext;

        registerPhases();
    }

    protected void registerPhases()
    {
        //RegistryLifecycleCallback callback = new RegistryLifecycleCallback();
        EmptyLifecycleCallback<AbstractRegistryBroker> callback = new EmptyLifecycleCallback<>();

        registerPhase(NotInLifecyclePhase.PHASE_NAME, NOT_IN_LIFECYCLE_PHASE, callback);
        registerPhase(Initialisable.PHASE_NAME, new MuleContextInitialisePhase(), callback);
        registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(), callback);
        registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(), callback);
        registerPhase(Disposable.PHASE_NAME, new MuleContextDisposePhase(), callback);
    }

    public RegistryLifecycleManager(String id, Registry object, Map<String, LifecyclePhase> phases )
    {
        super(id, object);
        RegistryLifecycleCallback callback = new RegistryLifecycleCallback();

        registerPhase(NotInLifecyclePhase.PHASE_NAME, NOT_IN_LIFECYCLE_PHASE, new LifecycleCallback(){
            public void onTransition(String phaseName, Object object) throws MuleException
            { }});

        for (Map.Entry<String, LifecyclePhase> entry : phases.entrySet())
        {
            registerPhase(entry.getKey(), entry.getValue(), callback);
        }
    }

    @Override
    protected void registerTransitions()
    {
        addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Initialisable.PHASE_NAME);
        addDirectTransition(Initialisable.PHASE_NAME, Startable.PHASE_NAME);

        //start stop
        addDirectTransition(Startable.PHASE_NAME, Stoppable.PHASE_NAME);
        addDirectTransition(Stoppable.PHASE_NAME, Startable.PHASE_NAME);
        //Dispose can be called from init or stopped
        addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Disposable.PHASE_NAME);
        addDirectTransition(Initialisable.PHASE_NAME, Disposable.PHASE_NAME);
        addDirectTransition(Stoppable.PHASE_NAME, Disposable.PHASE_NAME);

    }

    protected void registerPhase(String phaseName, LifecyclePhase phase)
    {
        phaseNames.add(phaseName);
        callbacks.put(phaseName, new RegistryLifecycleCallback());
        phases.put(phaseName, phase);
    }

    protected void registerPhase(String phaseName, LifecyclePhase phase, LifecycleCallback callback)
    {
        phaseNames.add(phaseName);
        callbacks.put(phaseName, callback);
        phases.put(phaseName, phase);
    }

    public void fireLifecycle(String destinationPhase) throws LifecycleException
    {
        checkPhase(destinationPhase);
        if (isDirectTransition(destinationPhase))
        {

            // transition to phase without going through other phases first
            invokePhase(destinationPhase, object, callbacks.get(destinationPhase));
        }
        else
        {
            //Call all phases to including the destination phase
            boolean start = false;
            for (String phase : phaseNames)
            {
                if (start)
                {
                    invokePhase(phase, object, callbacks.get(phase));
                    if (phase.equals(destinationPhase))
                    {
                        break;
                    }
                }
                if (phase.equals(getCurrentPhase()))
                {
                    start = true;
                }
            }
        }
    }

    protected void invokePhase(String phase, Object object, LifecycleCallback callback) throws LifecycleException
    {
        try
        {
            setExecutingPhase(phase);
            callback.onTransition(phase, object);
            setCurrentPhase(phase);
        }
        catch (LifecycleException e)
        {
            throw e;
        }
        catch (MuleException e)
        {
            throw new LifecycleException(CoreMessages.failedToInvokeLifecycle(phase, object), e);
        }
        finally
        {
            setExecutingPhase(null);
        }
    }


    //-------------------------------------------------------------------------------------------//
    //-                     LIFECYCLE HELPER METHODS
    //-------------------------------------------------------------------------------------------//


    public void applyPhase(Object object, String fromPhase, String toPhase) throws LifecycleException
    {
        //TODO i18n
        if(fromPhase == null || toPhase==null)
        {
            throw new IllegalArgumentException("toPhase and fromPhase must be null");
        }
        if(!phaseNames.contains(fromPhase))
        {
            throw new IllegalArgumentException("fromPhase '" + fromPhase + "' not a valid phase.");
        }
        if(!phaseNames.contains(toPhase))
        {
            throw new IllegalArgumentException("toPhase '" + fromPhase + "' not a valid phase.");
        }
        boolean start = false;
        for (String phaseName : phaseNames)
        {
            if(start)
            {
                phases.get(phaseName).applyLifecycle(object);
            }
            if(toPhase.equals(phaseName))
            {
                break;
            }
            if(phaseName.equals(fromPhase))
            {
                start = true;
            }

        }
    }

    public void applyCompletedPhases(Object object) throws LifecycleException
    {
        String lastPhase = NotInLifecyclePhase.PHASE_NAME;
        for (String phase : completedPhases)
        {
            if(isDirectTransition(lastPhase, phase))
            {
                LifecyclePhase lp = phases.get(phase);
                lp.applyLifecycle(object);
                lastPhase = phase;
            }
        }
    }

    class RegistryLifecycleCallback implements LifecycleCallback<Object>
    {
        /**
         * logger used by this class
         */
        protected transient final Log logger = LogFactory.getLog(RegistryLifecycleCallback.class);

        public void onTransition(String phaseName, Object object) throws MuleException
        {
            LifecyclePhase phase = phases.get(phaseName);

            if (logger.isDebugEnabled())
            {
                logger.debug("Applying lifecycle phase: " + phase + " for registry: " + object.getClass().getSimpleName());
            }

            if (phase instanceof ContainerManagedLifecyclePhase)
            {
                phase.applyLifecycle(object);
                return;
            }

            // overlapping interfaces can cause duplicates
            Set<Object> duplicates = new HashSet<Object>();

            for (LifecycleObject lo : phase.getOrderedLifecycleObjects())
            {
                // TODO Collection -> List API refactoring
                Collection<?> targetsObj = getLifecycleObject().lookupObjectsForLifecycle(lo.getType());
                List<Object> targets = new LinkedList<Object>(targetsObj);
                if (targets.size() == 0)
                {
                    continue;
                }

                lo.firePreNotification(muleContext);

                for (Iterator<Object> target = targets.iterator(); target.hasNext();)
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
    }
}
