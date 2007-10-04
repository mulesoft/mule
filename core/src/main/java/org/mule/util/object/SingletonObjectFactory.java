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

import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * Simple object factory which always returns the same object instance.
 */
public class SingletonObjectFactory extends SimpleObjectFactory implements Initialisable, Disposable
{
    private Object instance = null;

    /** For Spring only */
    public SingletonObjectFactory()
    {
        // nop
    }
    
    public SingletonObjectFactory(String objectClassName)
    {
        super(objectClassName);
    }

    public SingletonObjectFactory(Class objectClass)
    {
        super(objectClass);
    }

    public SingletonObjectFactory(Object instance)
    {
        this.instance = instance;
    }
    
    public void initialise() throws InitialisationException
    {
        if (instance == null)
        {
            try
            {
                instance = super.create();
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    public void dispose()
    {
        if (instance != null && instance instanceof Disposable)
        {
            ((Disposable) instance).dispose();
        }
    }

    /**
     * Always returns the same instance of the object.
     */
    public Object create() throws Exception
    {
        if (instance == null)
        {
            initialise();
        }
        return instance;
    }

    public Object getInstance()
    {
        return instance;
    }

    public void setInstance(Object instance)
    {
        this.instance = instance;
    }

}
