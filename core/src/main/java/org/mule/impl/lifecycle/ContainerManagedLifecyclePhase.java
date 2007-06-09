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

import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;

/**
 * A container-managed lifecycle phase is called by the container itself. This class acts as a marker to enable
 * tracking of phases even if Mule does not initiate the phase
 */
public class ContainerManagedLifecyclePhase extends LifecyclePhase
{
    public ContainerManagedLifecyclePhase(String name, Class lifecycleClass, String oppositePhaseName)
    {
        super(name, lifecycleClass, oppositePhaseName);
    }

    //@java.lang.Override
    public void fireLifecycle(UMOManagementContext managementContext, String currentPhase) throws UMOException
    {
        //The container manages this lifecycle
        return;
    }
}
