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

import org.mule.config.i18n.MessageFactory;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.InitialisationException;

import java.util.Map;

/**
 * Creates an instance of the object once and then always returns the same instance.
 */
public class SingletonObjectFactory extends AbstractObjectFactory 
{
    private Object instance = null;

    /** For Spring only */
    public SingletonObjectFactory() { super(); }
    
    public SingletonObjectFactory(String objectClassName) { super(objectClassName); }

    public SingletonObjectFactory(String objectClassName, Map properties) { super(objectClassName, properties); }

    public SingletonObjectFactory(Class objectClass) { super(objectClass); }

    public SingletonObjectFactory(Class objectClass, Map properties) { super(objectClass, properties); }
    
    /**
     * Create the singleton based on a previously created object.
     */
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
                instance = super.getOrCreate();
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    public void dispose()
    {
        //logger.debug("Disposing object instance");
        if (instance != null && instance instanceof Disposable)
        {
            ((Disposable) instance).dispose();
        }
        instance = null;
    }

    /**
     * Always returns the same instance of the object.
     */
    public Object getOrCreate() throws Exception
    {
        if (instance != null)
        {
            return instance;
        }
        else 
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Object factory has not been initialized."), this);
        }
    }

    /** {@inheritDoc} */
    public Object lookup(String id) throws Exception
    {
        // If IDs are specified, make sure they match.
        if (instance != null && instance instanceof Identifiable && id != null)
        {
            if (id.equals(((Identifiable) instance).getId()))
            {
                return instance;
            }
            else
            {
                return null;
            }
        }
        // Otherwise just return the singleton instance if it exists.
        else
        {
            return instance;
        }
    }

    /** {@inheritDoc} */
    public void release(Object object) throws Exception
    {
        // nothing to do for a singleton
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    ///////////////////////////////////////////////////////////////////////////////////////
    
    public Object getInstance()
    {
        return instance;
    }

    public void setInstance(Object instance)
    {
        this.instance = instance;
    }

}
