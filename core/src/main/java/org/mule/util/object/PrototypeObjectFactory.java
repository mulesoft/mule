/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleLogic;
import org.mule.api.lifecycle.LifecycleTransitionResult;

import java.util.Map;

/**
 * Creates a new instance of the object on each call.
 */
public class PrototypeObjectFactory extends AbstractObjectFactory
{

    /** For Spring only */
    public PrototypeObjectFactory()
    {
        super();
    }

    public PrototypeObjectFactory(String objectClassName)
    {
        super(objectClassName);
    }

    public PrototypeObjectFactory(String objectClassName, Map properties)
    {
        super(objectClassName, properties);
    }

    public PrototypeObjectFactory(Class objectClass)
    {
        super(objectClass);
    }

    public PrototypeObjectFactory(Class objectClass, Map properties)
    {
        super(objectClass, properties);
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        return LifecycleLogic.initialiseAll(this, super.initialise(), new LifecycleLogic.Closure()
        {
            public LifecycleTransitionResult doContinue()
            {
                return LifecycleTransitionResult.OK;
            }
        });
    }

    public void dispose()
    {
        // nothing to do
    }

    /**
     * Creates a new instance of the object on each call.
     */
    // @Override
    public Object getInstance() throws Exception
    {
        return super.getInstance();
    }

    /**
     * Disposes of the object if it is {@link Disposable}.
     */
    public void release(Object object) throws Exception
    {
        if (object instanceof Disposable)
        {
            ((Disposable) object).dispose();
        }
    }
}
