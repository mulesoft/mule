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
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.util.StringMessageUtils;

/**
 * This is a specialized class that extends {@link org.mule.lifecycle.RegistryLifecycleManager} and will
 * invoke lifecycle on the registry instance for the MuleContext.  This class must only be used by the MuleContext.
 */
public class MuleContextLifecycleManager extends AbstractLifecycleManager implements MuleContextAware
{
    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

//    public void fireLifecycle(String phase) throws LifecycleException
//    {
//        int current = getPhaseIndex(currentPhase);
//        int end = getPhaseIndex(phase);
//        LifecyclePhase li;
//
//        if(end < current)
//        {
//            li = getPhaseForIndex(end);
//            setExecutingPhase(li.getName());
//            muleContext.getRegistry().fireLifecycle(li.getName());
//            setCurrentPhase(li);
//            setExecutingPhase(null);
//            return;
//        }
//
//        //we want to start at the next one from current
//        current++;
//        while(current <= end)
//        {
//            li = getPhaseForIndex(current);
//            setExecutingPhase(li.getName());
//            muleContext.getRegistry().fireLifecycle(li.getName());
//            setCurrentPhase(li);
//            setExecutingPhase(null);
//            current++;
//        }
//    }

    protected void doApplyPhase(LifecyclePhase phase) throws LifecycleException
    {
        //phase.applyLifecycle(muleContext.getRegistry());
        muleContext.getRegistry().fireLifecycle(phase.getName());
    }
}