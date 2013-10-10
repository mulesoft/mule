/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.object;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;

import java.util.Map;

/**
 * Creates an instance of the object once and then always returns the same instance.
 */
public class SingletonObjectFactory extends AbstractObjectFactory
{
    private Object instance;

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
        this.instance = instance;
    }

    @Override
    public void dispose()
    {
        instance = null;
        super.dispose();
    }

    /**
     * Always returns the same instance of the object.
     * 
     * @param muleContext
     */
    @Override
    public Object getInstance(MuleContext muleContext) throws Exception
    {
        if (instance == null)
        {
            try
            {
                instance = super.getInstance(muleContext);
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
        return instance;
    }

    @Override
    public Class<?> getObjectClass()
    {
        if (instance != null)
        {
            return instance.getClass();
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
