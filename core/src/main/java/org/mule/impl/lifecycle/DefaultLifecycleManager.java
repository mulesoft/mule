/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.lifecycle;

import org.mule.impl.lifecycle.phases.ManagementContextStartPhase;
import org.mule.impl.lifecycle.phases.ManagementContextStopPhase;
import org.mule.impl.lifecycle.phases.TransientRegistryDisposePhase;
import org.mule.impl.lifecycle.phases.TransientRegistryInitialisePhase;
import org.mule.registry.Registry;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;

/**
 * Creates the default Mule lifecycleManager with {@link Initialisable#initialise()}, {@link Startable#start()},
 * {@link Stoppable#stop()} and {@link org.mule.umo.lifecycle.Disposable#dispose()}.
 *
 * @see org.mule.umo.lifecycle.Initialisable
 * @see org.mule.umo.lifecycle.Startable
 * @see org.mule.umo.lifecycle.Stoppable
 * @see org.mule.umo.lifecycle.Disposable
 */
public class DefaultLifecycleManager extends GenericLifecycleManager
{
    public DefaultLifecycleManager()
    {
        //Create Lifecycle phases
        Class[] ignorredObjects = new Class[]{Registry.class, UMOManagementContext.class};

        registerLifecycle(new TransientRegistryInitialisePhase(ignorredObjects));
        registerLifecycle(new ManagementContextStartPhase(ignorredObjects));
        registerLifecycle(new ManagementContextStopPhase(ignorredObjects));
        registerLifecycle(new TransientRegistryDisposePhase(ignorredObjects));
    }
}
