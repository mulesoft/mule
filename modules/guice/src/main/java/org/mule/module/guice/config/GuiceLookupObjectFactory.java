/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice.config;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.object.AbstractObjectFactory;

import com.google.inject.Injector;

/**
 * A Componet object factory that is configured from Guice
 */
public class GuiceLookupObjectFactory extends AbstractObjectFactory implements MuleContextAware
{
    private Class classBinding;
    private Injector injector;

    public void setMuleContext(MuleContext context)
    {
        try
        {
            //Grab a reference to the injector so that any lookups in this factory are Isolated. Using the registry means objects will be searched in all registries
            injector = context.getRegistry().lookupObject(Injector.class);
        }
        catch (RegistrationException e)
        {
            //Ignore will not happen
            logger.error(e.toString(), e);
        }
    }

    public void initialise() throws InitialisationException
    {
        if (classBinding == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Attribute @classBinding has not been set on the Guice component."), this);
        }

    }

    public void dispose()
    {
        // Not implemented for Spring Beans
    }

    @SuppressWarnings("unchecked")
    public Class<?> getObjectClass()
    {
        return injector.getInstance(classBinding).getClass();
    }

    public Object getInstance() throws Exception
    {
        Object instance = null;
        instance = injector.getInstance(classBinding);
        fireInitialisationCallbacks(instance);
        return instance;
    }

    public Class getClassBinding()
    {
        return classBinding;
    }

    public void setClassBinding(Class classBinding)
    {
        this.classBinding = classBinding;
    }

    @Override
    public boolean isSingleton()
    {
        //Guice will handle this
        return false;
    }

    @Override
    public boolean isExternallyManagedLifecycle()
    {
        //Guice doesn't provide lifecycle hooks
        return false;
    }
}
