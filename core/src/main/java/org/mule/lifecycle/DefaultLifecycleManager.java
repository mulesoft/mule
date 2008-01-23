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
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.Registry;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.lifecycle.phases.TransientRegistryDisposePhase;
import org.mule.lifecycle.phases.TransientRegistryInitialisePhase;

/**
 * Creates the default Mule lifecycleManager with {@link Initialisable#initialise()}, {@link Startable#start()},
 * {@link Stoppable#stop()} and {@link org.mule.api.lifecycle.Disposable#dispose()}.
 *
 * @see org.mule.api.lifecycle.Initialisable
 * @see org.mule.api.lifecycle.Startable
 * @see org.mule.api.lifecycle.Stoppable
 * @see org.mule.api.lifecycle.Disposable
 */
public class DefaultLifecycleManager extends GenericLifecycleManager
{
    public DefaultLifecycleManager()
    {
        //Create Lifecycle phases
        Class[] ignorredObjects = new Class[]{Registry.class, MuleContext.class};

        registerLifecycle(new TransientRegistryInitialisePhase(ignorredObjects));
        registerLifecycle(new MuleContextStartPhase(ignorredObjects));
        registerLifecycle(new MuleContextStopPhase(ignorredObjects));
        registerLifecycle(new TransientRegistryDisposePhase(ignorredObjects));
    }
}
