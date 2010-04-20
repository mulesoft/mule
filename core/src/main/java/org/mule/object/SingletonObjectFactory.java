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

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;

import java.lang.ref.SoftReference;
import java.util.Map;

/**
 * Creates an instance of the object once and then always returns the same instance.
 */
public class SingletonObjectFactory extends AbstractObjectFactory
{
    private SoftReference instance;

    /**
     * For Spring only
     */
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

    public SingletonObjectFactory(Class<?> objectClass, Map properties)
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

    @Override
    public void dispose()
    {
        if (instance != null)
        {
            instance.clear();
            instance.enqueue();
        }
        super.dispose();
    }

    /**
     * Always returns the same instance of the object.
     * @param muleContext
     */
    @Override
    public Object getInstance(MuleContext muleContext) throws Exception
    {
        if (instance == null || instance.get() == null)
        {
            try
            {
                instance = new SoftReference<Object>(super.getInstance(muleContext));
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
        return instance.get();
    }

    @Override
    public Class<?> getObjectClass()
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

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
