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
import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.registry.Registry;
import org.mule.util.StringMessageUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RegistryLifecycleManager extends AbstractLifecycleManager
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(RegistryLifecycleManager.class);

    private Registry registry;

    public RegistryLifecycleManager(Registry registry)
    {
        this.registry = registry;
    }

    protected void doApplyPhase(LifecyclePhase phase) throws LifecycleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Applying lifecycle phase: " + phase.getName());
        }

        if(phase instanceof ContainerManagedLifecyclePhase)
        {
            phase.applyLifecycle(registry);
            return;
        }

        // overlapping interfaces can cause duplicates
        Set duplicates = new HashSet();

        for (LifecycleObject lo : phase.getOrderedLifecycleObjects())
        {
            // TODO Collection -> List API refactoring
            Collection<?> targetsObj = registry.lookupObjects(lo.getType());
            List targets = new LinkedList(targetsObj);
            if (targets.size() == 0)
            {
                continue;
            }

            lo.firePreNotification(muleContext);

            for (Iterator target = targets.iterator(); target.hasNext();)
            {
                Object o = target.next();
                if (duplicates.contains(o))
                {
                    target.remove();
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("lifecycle phase: " + phase.getName() + " for object: " + o);
                    }
                    phase.applyLifecycle(o);
                    target.remove();
                    duplicates.add(o);
                }
            }

            lo.firePostNotification(muleContext);
        }

    }
}
