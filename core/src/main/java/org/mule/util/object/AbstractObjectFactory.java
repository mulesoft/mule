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

import org.mule.config.ConfigurationException;
import org.mule.impl.UMOComponentAware;
import org.mule.umo.UMOComponent;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates object instances based on the class and sets any properties.
 */
public abstract class AbstractObjectFactory implements ObjectFactory, UMOComponentAware
{
    public static final String ATTRIBUTE_OBJECT_CLASS_NAME = "objectClassName";
    public static final String ATTRIBUTE_OBJECT_CLASS = "objectClass";
    
    protected String objectClassName;
    protected Class objectClass = null;
    protected Map properties = null;
    
    /**
     * This is not pretty but its the only way I could find to get the UMOComponent
     * injected into each instance of a POJO service.
     */
    UMOComponent component;
    
    protected transient Log logger = LogFactory.getLog(getClass());
    
    /** For Spring only */
    public AbstractObjectFactory()
    {
        // nop
    }
    
    public AbstractObjectFactory(String objectClassName)
    {
        this(objectClassName, null);
    }

    public AbstractObjectFactory(String objectClassName, Map properties)
    {
        this.objectClassName = objectClassName;
        this.properties = properties;
    }

    public AbstractObjectFactory(Class objectClass)
    {
        this(objectClass, null);
    }

    public AbstractObjectFactory(Class objectClass, Map properties)
    {
        this.objectClass = objectClass;
        this.properties = properties;
    }

    /**
     * Creates an initialized object instance based on the class and sets any properties.
     */
    public Object getOrCreate() throws Exception
    {
        if (objectClass == null)
        {
            objectClass = ClassUtils.getClass(objectClassName);
        }
        Object object = ClassUtils.instanciateClass(objectClass, ClassUtils.NO_ARGS);

        if (properties != null)
        {
            BeanUtils.populate(object, properties);            
        }
        
        // This is not pretty but its the only way I could find to get the UMOComponent
        // properly injected into each instance of a POJO service.
        if (component != null)
        {
            BeanUtils.setProperty(object, "component", component);
        }

        if (object instanceof Initialisable)
        {
            ((Initialisable) object).initialise();
        }
        return object;
    }

    /**
     * This is not pretty but its the only way I could find to get the UMOComponent
     * injected into each instance of a POJO service.
     */
    public void setComponent(UMOComponent component) throws ConfigurationException
    {
        this.component = component;
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


