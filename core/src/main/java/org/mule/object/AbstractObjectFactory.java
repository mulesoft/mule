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
import org.mule.api.lifecycle.InitialisationCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates object instances based on the class and sets any properties.  This factory is also responsible for applying
 * any object processors on the object before the lifecycle callbacks are called.
 */
public abstract class AbstractObjectFactory implements ObjectFactory, ServiceAware
{
    public static final String ATTRIBUTE_OBJECT_CLASS_NAME = "objectClassName";
    public static final String ATTRIBUTE_OBJECT_CLASS = "objectClass";

    protected String objectClassName;
    protected SoftReference<Class<?>> objectClass = null;
    protected Map properties = null;
    protected List<InitialisationCallback> initialisationCallbacks = new ArrayList<InitialisationCallback>();
    protected Service service;
    protected boolean disposed = false;

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
        super();
        this.objectClassName = objectClassName;
        this.properties = properties;
        setupObjectClassFromObjectClassName();
    }

    public AbstractObjectFactory(Class<?> objectClass)
    {
        this(objectClass, null);
    }

    public AbstractObjectFactory(Class<?> objectClass, Map properties)
    {
        super();
        this.objectClassName = objectClass.getName();
        this.objectClass = new SoftReference<Class<?>>(objectClass);
        this.properties = properties;
    }

    protected Class<?> setupObjectClassFromObjectClassName()
    {
        try
        {
            Class<?> klass = ClassUtils.getClass(objectClassName);
            objectClass = new SoftReference<Class<?>>(klass);
            return klass;
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public void setService(Service service)
    {
        this.service = service;
    }

    public void initialise() throws InitialisationException
    {
        if ((objectClassName == null) || (objectClass == null))
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Object factory has not been initialized."), this);
        }
        disposed=false;
    }
    
    public void dispose()
    {
        disposed=true;
        //Don't reset the component config state i.e. objectClass since service objects can be recycled
    }

    /**
     * Creates an initialized object instance based on the class and sets any properties.
     * This method handles all injection of properties for the resulting object
     * @param muleContext the current {@link org.mule.api.MuleContext} instance. This can be used for performing registry lookups
     * applying processors to newly created objects or even firing custom notifications
     * @throws Exception Can throw any type of exception while creating a new object
     */
    public Object getInstance(MuleContext muleContext) throws Exception
    {
        if (objectClass == null || disposed)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Object factory has not been initialized."), this);
        }
        
        Class<?> klass = objectClass.get();
        if (klass == null)
        {
            klass = setupObjectClassFromObjectClassName();
        }

        Object object = ClassUtils.instanciateClass(klass);

        if (properties != null)
        {
            BeanUtils.populateWithoutFail(object, properties, true);
        }

        if(object instanceof ServiceAware)
        {
            ((ServiceAware)object).setService(service);
        }

        if(isAutoWireObject())
        {
            muleContext.getRegistry().applyProcessors(object);
        }
        fireInitialisationCallbacks(object);
        
        return object;
    }

    protected void fireInitialisationCallbacks(Object component) throws InitialisationException
    {
        for (InitialisationCallback callback : initialisationCallbacks)
        {
            callback.initialise(component);
        }
    }

    public Class<?> getObjectClass()
    {
        if (objectClass != null)
        {
            Class<?> klass = objectClass.get();
            if (klass == null)
            {
                klass = setupObjectClassFromObjectClassName();
            }
            return klass;
        }

        // objectClass may be null if this factory was not yet initialized or was disposed
        return null;
    }

    public void setObjectClass(Class<?> objectClass)
    {
        this.objectClass = new SoftReference<Class<?>>(objectClass);
        this.objectClassName = objectClass.getName();
    }

    protected String getObjectClassName()
    {
        return objectClassName;
    }

    public void setObjectClassName(String objectClassName)
    {
        this.objectClassName = objectClassName;
        setupObjectClassFromObjectClassName();
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

    public boolean isAutoWireObject()
    {
        return true;
    }
}
