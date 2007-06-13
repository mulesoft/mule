/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;



/**
 * Simple object factory which always returns the same object instance.
 */
public class SingletonObjectFactory extends SimpleObjectFactory
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
    
    /**
     * Always returns the same instance of the object.
     */
    public Object create() throws Exception
    {
        if (instance == null)
        {
            instance = super.create();
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
