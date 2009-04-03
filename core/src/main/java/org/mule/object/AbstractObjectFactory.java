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

import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates object instances based on the class and sets any properties.
 */
public abstract class AbstractObjectFactory implements ObjectFactory
{
    public static final String ATTRIBUTE_OBJECT_CLASS_NAME = "objectClassName";
    public static final String ATTRIBUTE_OBJECT_CLASS = "objectClass";

    protected String objectClassName;
    protected SoftReference<Class> objectClass = null;
    protected Map properties = null;
    protected List initialisationCallbacks = new ArrayList();

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
        this.objectClass = new SoftReference<Class>(objectClass);
        this.properties = properties;
    }

    public void initialise() throws InitialisationException
    {
        if ((objectClass == null || (objectClass.get() == null)) && objectClassName == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Object factory has not been initialized."), this);
        }

        if ((objectClass == null || (objectClass.get() == null)) && objectClassName != null)
        {
            try
            {
                objectClass = new SoftReference<Class>(ClassUtils.getClass(objectClassName));
            }
            catch (ClassNotFoundException e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    public void dispose()
    {
        this.objectClass.clear();
        this.objectClass.enqueue();
        this.objectClassName = null;
    }

    /**
     * Creates an initialized object instance based on the class and sets any properties.
     */
    public Object getInstance() throws Exception
    {
        if (objectClass == null || objectClass.get() == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Object factory has not been initialized."), this);
        }

        Object object = ClassUtils.instanciateClass(objectClass.get());

        if (properties != null)
        {
            BeanUtils.populateWithoutFail(object, properties, true);
        }

        fireInitialisationCallbacks(object);
        
        return object;
    }
    
    protected void fireInitialisationCallbacks(Object component) throws InitialisationException
    {
        InitialisationCallback callback;
        for (Iterator iterator = initialisationCallbacks.iterator(); iterator.hasNext();)
        {
            callback = (InitialisationCallback) iterator.next();
            callback.initialise(component);
        }
    }

    public Class getObjectClass()
    {
        return objectClass.get();
    }

    public void setObjectClass(Class objectClass)
    {
        this.objectClass = new SoftReference<Class>(objectClass);
    }

    protected String getObjectClassName()
    {
        return objectClassName;
    }

    public void setObjectClassName(String objectClassName)
    {
        this.objectClassName = objectClassName;
    }

    protected Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }
    
    public void addObjectInitialisationCallback(InitialisationCallback callback)
    {
        initialisationCallbacks.add(callback);
    }
    
    public boolean isSingleton()
    {
        return false;
    }

    public boolean isExternallyManagedLifecycle()
    {
        return false;
    }

}
