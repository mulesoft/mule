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
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.Registry;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.lifecycle.RegistryLifecycleManager;
import org.mule.lifecycle.phases.ContainerManagedLifecyclePhase;
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
        registerPhase(NotInLifecyclePhase.PHASE_NAME, NOT_IN_LIFECYCLE_PHASE,
                      new EmptyLifecycleCallback<AbstractRegistryBroker>());
        registerPhase(Initialisable.PHASE_NAME, new SpringContextInitialisePhase());
        registerPhase(Startable.PHASE_NAME, new MuleContextStartPhase(),
            new EmptyLifecycleCallback<AbstractRegistryBroker>());
        registerPhase(Stoppable.PHASE_NAME, new MuleContextStopPhase(),
            new EmptyLifecycleCallback<AbstractRegistryBroker>());
        registerPhase(Disposable.PHASE_NAME, new SpringContextDisposePhase());
    }

    // ///////////////////////////////////////////////////////////////////////////////////
    // Spring custom lifecycle phases
    // ///////////////////////////////////////////////////////////////////////////////////

    /**
     * A lifecycle phase that will delegate any lifecycle invocations to a container
     * such as Spring or Guice
     */
    class SpringContextInitialisePhase extends ContainerManagedLifecyclePhase
    {
        public SpringContextInitialisePhase()
        {
            super(Initialisable.PHASE_NAME, Initialisable.class, Disposable.PHASE_NAME);
            registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);
        }
    }

    /**
     * A lifecycle phase that will delegate to the
     * {@link org.mule.config.spring.SpringRegistry#doDispose()} method which in turn
     * will destroy the application context managed by this registry
     */
    class SpringContextDisposePhase extends ContainerManagedLifecyclePhase
    {
        public SpringContextDisposePhase()
        {
            super(Disposable.PHASE_NAME, Disposable.class, Initialisable.PHASE_NAME);
            registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);
            // You can dispose from all phases
            registerSupportedPhase(LifecyclePhase.ALL_PHASES);
        }

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
