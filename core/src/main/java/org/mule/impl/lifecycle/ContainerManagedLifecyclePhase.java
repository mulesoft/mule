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

/**
 * TODO
 */
public class ContainerManagedLifecyclePhase extends LifecyclePhase
{
    public ContainerManagedLifecyclePhase(String name, Class lifecycleClass)
    {
        super(name, lifecycleClass);
    }

    //@java.lang.Override
    public void callLifecycle(String currentPhase) throws UMOException
    {
        //The container manages this lifecycle
        return;
    }
}
