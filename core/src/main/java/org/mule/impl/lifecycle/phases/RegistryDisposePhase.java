/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.lifecycle.phases;

import org.mule.impl.internal.notifications.RegistryNotification;
import org.mule.impl.lifecycle.LifecyclePhase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.UMOLifecyclePhase;

/**
 * TODO
 */
public class RegistryDisposePhase extends LifecyclePhase
{
    public RegistryDisposePhase()
    {
        super(Disposable.PHASE_NAME, Disposable.class);
        registerSupportedPhase(UMOLifecyclePhase.ALL_PHASES);
    }

    //@java.lang.Override
    public void fireLifecycle(UMOManagementContext managementContext, String currentPhase) throws UMOException
    {
        managementContext.fireNotification(new RegistryNotification(managementContext.getRegistry(), RegistryNotification.REGISTRY_DISPOSED));
    }
}