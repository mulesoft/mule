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

import org.mule.impl.lifecycle.LifecyclePhase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;

/**
 * Objects are disposed of via the Registry since the Registry manages the creation/initialisation of the objects
 * it must also take care of disposing them. However, a user may want to initiate a dispose via the
 * {@link org.mule.impl.ManagementContext} so the dispose Lifecycle phase for the {@link org.mule.impl.ManagementContext}
 * needs to call dispose on the Registry.
 */
public class ManagementContextDisposePhase extends LifecyclePhase
{
    public ManagementContextDisposePhase()
    {
        super(Disposable.PHASE_NAME, Disposable.class, Initialisable.PHASE_NAME);
    }

    public void fireLifecycle(UMOManagementContext managementContext, String currentPhase) throws UMOException
    {
        //Delegate this to the Registry
        if (managementContext.getRegistry() != null)
        {
            managementContext.getRegistry().dispose();
        }
    }
}
