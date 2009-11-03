/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.fruit;

import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;

/**
 * <code>BananaFactory</code> is a test factory that creates Bananas
 */
public class BananaFactory implements ObjectFactory
{
    public void initialise() throws InitialisationException
    {
        // nothing to do
    }
    
    public void dispose()
    {
        // nothing to do
    }

    public Object getInstance() throws Exception
    {
        return new Banana();
    }

    public Class<?> getObjectClass()
    {
        return Banana.class;
    }

    public void addObjectInitialisationCallback(InitialisationCallback callback)
    {
        throw new UnsupportedOperationException();
    }

    public boolean isSingleton()
    {
        return false;
    }

    public boolean isExternallyManagedLifecycle()
    {
        return false;
    }

    public boolean isAutoWireObject()
    {
        return false;
    }
}
