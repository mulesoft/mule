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

public class LifecycleUtils
{

    private LifecycleUtils()
    {
    }

    /**
     * Invokes {@link Initialisable#initialise()} on {@code object}
     * if it implements the {@link Initialisable} interface.
     *
     * @param object the object you're trying to initialise
     * @throws InitialisationException
     */
    public static void initialiseIfNeeded(Object object) throws InitialisationException
    {
        initialiseIfNeeded(object, null);
    }

    /**
     * The same as {@link #initialiseIfNeeded(Object)}, only that before checking
     * for {@code object} being {@link Initialisable}, it also checks if it implements
     * {@link MuleContextAware}, in which case it will invoke {@link MuleContextAware#setMuleContext(MuleContext)}
     * with the given {@code muleContext}
     *
     * @param object      the object you're trying to initialise
     * @param muleContext a {@link MuleContext}
     * @throws InitialisationException
     */
    public static void initialiseIfNeeded(Object object, MuleContext muleContext) throws InitialisationException
    {
        if (muleContext != null && object instanceof MuleContextAware)
        {
            ((MuleContextAware) object).setMuleContext(muleContext);
        }

        if (object instanceof Initialisable)
        {
            ((Initialisable) object).initialise();
        }
    }


    /**
     * For each item in the {@code objects} collection, it invokes {@link Initialisable#initialise()}
     * if it implements the {@link Initialisable} interface.
     *
     * @param objects the list of objects to be initialised
     * @throws InitialisationException
     */
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

    /**
     * Invokes {@link Startable#start()} on {@code object} if it implements the
     * {@link Startable} interface
     *
     * @param object the object you're trying to start
     * @throws MuleException
     */
    public static void startIfNeeded(Object object) throws MuleException
    {
        if (object instanceof Startable)
        {
            ((Startable) object).start();
        }
    }

    /**
     * For each item in the {@code objects} collection, it invokes the the {@link Startable#start()}
     * if it implements the {@link Startable} interface.
     *
     * @param objects the list of objects to be started
     * @throws MuleException
     */
    public static void startIfNeeded(Collection<? extends Object> objects) throws MuleException
    {
        doApplyPhase(Startable.PHASE_NAME, objects, null);
    }

    /**
     * For each item in the {@code objects} collection, it invokes the {@link Stoppable#stop()}
     * if it implements the {@link Stoppable} interface.
     *
     * @param objects the list of objects to be stopped
     * @throws MuleException
     */
    public static void stopIfNeeded(Collection<? extends Object> objects) throws MuleException
    {
        doApplyPhase(Stoppable.PHASE_NAME, objects, null);
    }

    /**
     * Invokes the {@link Stoppable#stop()} on {@code object} if it implements the {@link Stoppable} interface.
     *
     * @param object the object you're trying to stop
     * @throws MuleException
     */
    public static void stopIfNeeded(Object object) throws MuleException
    {
        doApplyPhase(Stoppable.PHASE_NAME, Arrays.asList(object), null);
    }

    /**
     * Invokes {@link Disposable#dispose()} on {@code object} if it implements the
     * {@link Disposable} interface. If the dispose operation fails, then the exception
     * will be silently logged using the provided {@code logger}
     *
     * @param object the object you're trying to dispose
     */
    public static void disposeIfNeeded(Object object, Logger logger)
    {
        disposeAllIfNeeded(Arrays.asList(object), logger);
    }

    /**
     * For each item in the {@code objects} collection, it invokes {@link Disposable#dispose()}
     * if it implements the {@link Disposable} interface.
     * <p/>
     * Per each dispose operation that fails, the exception will be silently logged using the
     * provided {@code logger}
     *
     * @param objects the list of objects to be stopped
     * @throws MuleException
     */
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
