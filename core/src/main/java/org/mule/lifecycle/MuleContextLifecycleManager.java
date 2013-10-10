/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.lifecycle.phases.NotInLifecyclePhase;

/**
 * This is a specialized class that extends {@link RegistryLifecycleManager} and will
 * invoke lifecycle on the registry instance for the MuleContext.  This class must only be used by the MuleContext.
 */
public class MuleContextLifecycleManager extends AbstractLifecycleManager<MuleContext> implements MuleContextAware
{

    private MuleContext muleContext;

    private MuleContextLifecycleCallback callback = new MuleContextLifecycleCallback();

    public MuleContextLifecycleManager()
    {
        //We cannot pass in a MuleContext on creation since the context is not actually created when this object is needed
        super("MuleContext", null);
    }

    @Override
    protected void registerTransitions()
    {
        //init dispose
        addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Initialisable.PHASE_NAME);
        addDirectTransition(NotInLifecyclePhase.PHASE_NAME, Disposable.PHASE_NAME);
        addDirectTransition(Initialisable.PHASE_NAME, Startable.PHASE_NAME);
        addDirectTransition(Initialisable.PHASE_NAME, Disposable.PHASE_NAME);

        //start stop
        addDirectTransition(Startable.PHASE_NAME, Stoppable.PHASE_NAME);
        addDirectTransition(Stoppable.PHASE_NAME, Startable.PHASE_NAME);
        addDirectTransition(Stoppable.PHASE_NAME, Disposable.PHASE_NAME);
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        this.object = muleContext;
    }

    public void fireLifecycle(String destinationPhase) throws LifecycleException
    {
        checkPhase(destinationPhase);
        invokePhase(destinationPhase, object, callback);
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
            throw new LifecycleException(e, this);
        }
        finally
        {
            setExecutingPhase(null);
        }

    }

    class MuleContextLifecycleCallback implements LifecycleCallback<MuleContext>
    {
        public void onTransition(String phaseName, MuleContext muleContext) throws MuleException
        {
            muleContext.getRegistry().fireLifecycle(phaseName);
        }
    }
}
