/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.lifecycle;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

public abstract class LifecycleUtils
{

    public static void initialiseIfNeeded(Object object, MuleContext muleContext) throws InitialisationException
    {
        if (object instanceof MuleContextAware)
        {
            ((MuleContextAware) object).setMuleContext(muleContext);
        }

        if (object instanceof Initialisable)
        {
            ((Initialisable) object).initialise();
        }
    }

    public static void initialiseIfNeeded(Collection<? extends Object> objects) throws InitialisationException
    {
        try
        {
            doApplyPhase(Initialisable.PHASE_NAME, objects, null);
        }
        catch (MuleException e)
        {
            throw (InitialisationException) e;
        }
    }

    public static void startIfNeeded(Collection<? extends Object> objects) throws MuleException
    {
        doApplyPhase(Startable.PHASE_NAME, objects, null);
    }

    public static void stopIfNeeded(Collection<? extends Object> objects) throws MuleException
    {
        doApplyPhase(Stoppable.PHASE_NAME, objects, null);
    }

    public static void stopIfNeeded(Object object) throws MuleException
    {
        doApplyPhase(Stoppable.PHASE_NAME, Arrays.asList(object), null);
    }

    public static void disposeIfNeeded(Object object, Logger logger)
    {
        disposeAllIfNeeded(Arrays.asList(object), logger);
    }

    public static void disposeAllIfNeeded(Collection<? extends Object> objects, Logger logger)
    {
        try
        {
            doApplyPhase(Disposable.PHASE_NAME, objects, logger);
        }
        catch (MuleException e)
        {
            logger.error("Exception found trying to dispose object. Shutdown will continue", e);
        }
    }

    private static void doApplyPhase(String phase, Collection<? extends Object> objects, Logger logger) throws MuleException
    {
        if (CollectionUtils.isEmpty(objects))
        {
            return;
        }

        for (Object object : objects)
        {
            if (object == null)
            {
                continue;
            }

            try
            {
                if (Initialisable.PHASE_NAME.equals(phase) && object instanceof Initialisable)
                {
                    ((Initialisable) object).initialise();
                }
                else if (Startable.PHASE_NAME.equals(phase) && object instanceof Startable)
                {
                    ((Startable) object).start();
                }
                else if (Stoppable.PHASE_NAME.equals(phase) && object instanceof Stoppable)
                {
                    ((Stoppable) object).stop();
                }
                else if (Disposable.PHASE_NAME.equals(phase) && object instanceof Disposable)
                {
                    ((Disposable) object).dispose();
                }
            }
            catch (MuleException e)
            {
                if (logger != null)
                {
                    logger.error(String.format("Could not apply phase %s on object of class %s", phase, object.getClass().getName()), e);
                }
                else
                {
                    throw e;
                }
            }
        }
    }
}
