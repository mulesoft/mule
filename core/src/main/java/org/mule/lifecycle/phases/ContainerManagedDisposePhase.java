/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle.phases;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.registry.Registry;
import org.mule.lifecycle.ContainerManagedLifecyclePhase;

/**
 * A lifecycle phase that will delegate any lifecycle invocations to a container such as Spring or Guice
 */
public class ContainerManagedDisposePhase extends ContainerManagedLifecyclePhase
{
    public ContainerManagedDisposePhase()
    {
        super(Disposable.PHASE_NAME, Disposable.class, Initialisable.PHASE_NAME);
        registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);
        //You can dispose from all phases
        registerSupportedPhase(LifecyclePhase.ALL_PHASES);
    }

    @Override
    public void applyLifecycle(Object o) throws LifecycleException
    {
        //delegate to the container registry
        ((Registry)o).dispose();
    }
}