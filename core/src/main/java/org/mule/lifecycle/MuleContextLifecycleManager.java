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

import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;

/**
 * This is a specialized class that extends {@link org.mule.lifecycle.RegistryLifecycleManager} and will
 * invoke lifecycle on the registry instance for the MuleContext.  This class must only be used by the MuleContext.
 */
public class MuleContextLifecycleManager extends AbstractLifecycleManager
{
    @Override
    protected void doApplyPhase(LifecyclePhase phase) throws LifecycleException
    {
        muleContext.getRegistry().fireLifecycle(phase.getName());
    }
}
