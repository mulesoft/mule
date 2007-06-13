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

import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;

import java.util.Map;

/**
 * Simple object factory based on a class name and default constructor.
 */
public class SimpleObjectFactory implements ObjectFactory
{
    String objectClassName;
    Class objectClass = null;
    Map properties = null;
    
    /** For Spring only */
    public SimpleObjectFactory()
    {
        // nop
    }
    
    public SimpleObjectFactory(String objectClassName)
    {
        this(objectClassName, null);
    }

    public SimpleObjectFactory(String objectClassName, Map properties)
    {
        this.objectClassName = objectClassName;
        this.properties = properties;
    }

    public SimpleObjectFactory(Class objectClass)
    {
        this(objectClass, null);
    }

    public SimpleObjectFactory(Class objectClass, Map properties)
    {
        this.objectClass = objectClass;
        this.properties = properties;
    }

    /**
     * Creates a new instance of the object on each call.
     */
    public Object create() throws Exception
    {
        if (objectClass == null)
        {
            objectClass = ClassUtils.getClass(objectClassName);
        }
        Object component = ClassUtils.instanciateClass(objectClass, ClassUtils.NO_ARGS);

        if (properties != null)
        {
            BeanUtils.populate(component, properties);
        }
        return component;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    ///////////////////////////////////////////////////////////////////////////////////////
    
    public Class getObjectClass()
    {
        return objectClass;
    }

    public void setObjectClass(Class objectClass)
    {
        this.objectClass = objectClass;
    }

    public String getObjectClassName()
    {
        return objectClassName;
    }

    public void setObjectClassName(String objectClassName)
    {
        this.objectClassName = objectClassName;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }
}
