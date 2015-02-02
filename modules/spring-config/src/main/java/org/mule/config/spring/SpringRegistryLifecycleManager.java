/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.Registry;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.lifecycle.RegistryLifecycleManager;
import org.mule.lifecycle.phases.MuleContextDisposePhase;
import org.mule.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.lifecycle.phases.NotInLifecyclePhase;
import org.mule.registry.AbstractRegistryBroker;

public class SpringRegistryLifecycleManager extends RegistryLifecycleManager
{
    public SpringRegistryLifecycleManager(String id, Registry object, MuleContext muleContext)
    {
        super(id, object, muleContext);
    }

    protected void registerPhases()
    {
        final LifecycleCallback<AbstractRegistryBroker> emptyCallback = new EmptyLifecycleCallback<>();
        registerPhase(NotInLifecyclePhase.PHASE_NAME, NOT_IN_LIFECYCLE_PHASE, emptyCallback);
        registerPhase(Initialisable.PHASE_NAME, new SpringContextInitialisePhase());
        registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(), emptyCallback);
        registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(), emptyCallback);
        registerPhase(Disposable.PHASE_NAME, new SpringContextDisposePhase());
    }

    // ///////////////////////////////////////////////////////////////////////////////////
    // Spring custom lifecycle phases
    // ///////////////////////////////////////////////////////////////////////////////////

    class SpringContextInitialisePhase extends MuleContextInitialisePhase
    {
        @Override
        public void applyLifecycle(Object o) throws LifecycleException
        {
            if (o instanceof SpringRegistry)
            {
                return;
            }

            super.applyLifecycle(o);
        }
    }

    /**
     * A lifecycle phase that will delegate to the
     * {@link org.mule.config.spring.SpringRegistry#doDispose()} method which in turn
     * will destroy the application context managed by this registry
     */
    class SpringContextDisposePhase extends MuleContextDisposePhase
    {

        @Override
        public void applyLifecycle(Object o) throws LifecycleException
        {
            if (o instanceof SpringRegistry)
            {
                ((SpringRegistry) o).doDispose();
            }
            else
            {
                super.applyLifecycle(o);
            }
        }
    }

}
