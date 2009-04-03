/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.object;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.MessageFactory;

import java.lang.ref.SoftReference;
import java.util.Map;

/**
 * Creates an instance of the object once and then always returns the same instance.
 */
public class SingletonObjectFactory extends AbstractObjectFactory
{
    private SoftReference instance;

    /** For Spring only */
    public SingletonObjectFactory()
    {
        super();
    }

    public SingletonObjectFactory(String objectClassName)
    {
        super(objectClassName);
    }

    public SingletonObjectFactory(String objectClassName, Map properties)
    {
        super(objectClassName, properties);
    }

    public SingletonObjectFactory(Class objectClass)
    {
        super(objectClass);
    }

    public SingletonObjectFactory(Class objectClass, Map properties)
    {
        super(objectClass, properties);
    }

    /**
     * Create the singleton based on a previously created object.
     */
    public SingletonObjectFactory(Object instance)
    {
        super(instance.getClass());
        this.instance = new SoftReference<Object>(instance);
    }

    // @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (instance == null || instance.get() == null)
        {
            try
            {
                instance = new SoftReference<Object>(super.getInstance());
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    // @Override
    public void dispose()
    {
        instance.clear();
        instance.enqueue();
        super.dispose();
    }

    /**
     * Always returns the same instance of the object.
     */
    // @Override
    public Object getInstance() throws Exception
    {
        if (instance != null && instance.get() != null)
        {
            return instance.get();
        }
        else
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Object factory has not been initialized."), this);
        }
    }

    // @Override
    public Class getObjectClass()
    {
        if (instance != null && instance.get() != null)
        {
            return instance.get().getClass();
        }
        else
        {
            return super.getObjectClass();
        }
    }

    public boolean isSingleton()
    {
        return true;
    }

}
