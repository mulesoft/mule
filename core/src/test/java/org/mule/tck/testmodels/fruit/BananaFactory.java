/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;

/**
 * <code>BananaFactory</code> is a test factory that creates Bananas
 */
public class BananaFactory implements ObjectFactory
{
    @Override
    public void initialise() throws InitialisationException
    {
        // nothing to do
    }

    @Override
    public void dispose()
    {
        // nothing to do
    }

    @Override
    public Object getInstance(MuleContext muleContext) throws Exception
    {
        return new Banana();
    }

    @Override
    public Class<?> getObjectClass()
    {
        return Banana.class;
    }

    @Override
    public void addObjectInitialisationCallback(InitialisationCallback callback)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    @Override
    public boolean isExternallyManagedLifecycle()
    {
        return false;
    }

    @Override
    public boolean isAutoWireObject()
    {
        return false;
    }
}
