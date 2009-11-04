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
import org.mule.lifecycle.ContainerManagedLifecyclePhase;

/**
 * Since all objects are created and initialised in the registry, the Initialise lifecyclePhase is always
 * taken care of by the Registry, hence this class extends {@link org.mule.lifecycle.ContainerManagedLifecyclePhase}
 */
public class MuleContextInitialisePhase extends ContainerManagedLifecyclePhase
{
    public MuleContextInitialisePhase()
    {
        super(Initialisable.PHASE_NAME, Initialisable.class, Disposable.PHASE_NAME);
        registerSupportedPhase(NotInLifecyclePhase.PHASE_NAME);
    }
}