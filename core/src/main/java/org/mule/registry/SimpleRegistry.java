/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.InitialisingRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.lifecycle.phases.NotInLifecyclePhase;

import java.lang.reflect.Field;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple implementation of {@link InitialisingRegistry}. Useful for starting really lightweight
 * contexts which don't depend on heavier object containers such as Spring or Guice (testing being
 * the best example). <b>Do not use this registry for heavy duty production environments</b>.
 * <p/>
 * The {@link #inject(Object)} operation will only consider fields annotated with {@link Inject} and will perform
 * the injection using simple, not-cached reflection. Again, not recommended for production environments.
 *
 * @since 3.7.0
 */
public class SimpleRegistry extends TransientRegistry implements InitialisingRegistry
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRegistry.class);

    public SimpleRegistry(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        injectFieldDependencies();
        initialiseObjects();
        super.doInitialise();
    }

    @Override
    protected void doRegisterObject(String key, Object object, Object metadata) throws RegistrationException
    {
        Object previous = doGet(key);
        if (previous != null)
        {
            unregisterObject(key);
        }

        doPut(key, object);

        try
        {
            getLifecycleManager().applyCompletedPhases(object);
        }
        catch (MuleException e)
        {
            throw new RegistrationException(e);
        }
    }

    @Override
    public Object applyLifecycle(Object object) throws MuleException
    {
        getLifecycleManager().applyCompletedPhases(object);
        return object;
    }

    @Override
    public Object applyLifecycle(Object object, String phase) throws MuleException
    {
        if (phase == null)
        {
            getLifecycleManager().applyCompletedPhases(object);
        }
        else
        {
            getLifecycleManager().applyPhase(object, NotInLifecyclePhase.PHASE_NAME, phase);
        }
        return object;
    }

    @Override
    public Object inject(Object object)
    {
        return applyProcessors(object, null);
    }

    private void injectFieldDependencies() throws InitialisationException
    {
        for (Object object : lookupObjects(Object.class))
        {
            injectInto(object);
        }
    }

    private void injectInto(Object object)
    {
        for (Field field : getAllFields(object.getClass(), withAnnotation(Inject.class)))
        {
            Class<?> dependencyType = field.getType();
            try
            {
                Object dependency = lookupObject(dependencyType);
                if (dependency != null)
                {
                    field.setAccessible(true);
                    field.set(object, dependency);
                }
            }
            catch (Exception e)
            {
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug(String.format("Could not inject dependency on field %s of type %s",
                                               field.getName(), object.getClass().getName()), e);
                }
            }
        }
    }

    private void initialiseObjects() throws InitialisationException
    {
        try
        {
            for (Initialisable initialisable : lookupObjects(Initialisable.class))
            {
                getLifecycleManager().applyPhase(initialisable, NotInLifecyclePhase.PHASE_NAME, Initialisable.PHASE_NAME);
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }
}
