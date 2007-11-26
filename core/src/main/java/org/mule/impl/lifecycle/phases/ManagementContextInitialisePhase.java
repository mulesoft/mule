/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.lifecycle.phases;

import org.mule.impl.lifecycle.ContainerManagedLifecyclePhase;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;

/**
 * Since all objects are created and initialised in the registry, the Initialise lifecyclePhase is always
 * taken care of by the Registry, hence this class extends {@link org.mule.impl.lifecycle.ContainerManagedLifecyclePhase}
 */
public class ManagementContextInitialisePhase extends ContainerManagedLifecyclePhase
{
    public ManagementContextInitialisePhase()
    {
        super(Initialisable.PHASE_NAME, Initialisable.class, Disposable.PHASE_NAME);
    }
}