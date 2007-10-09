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
import org.mule.util.UUID;

import java.util.Iterator;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

/**
 * Creates a new instance of the object on each call.  If the object implements the Identifiable 
 * interface, individual instances can be looked up by ID.
 */
public class PrototypeObjectFactory extends AbstractObjectFactory
{
    /** 
     * Active instances of the object which have been created.  
     */
    protected Map instances = null;
    
    /** For Spring only */
    public PrototypeObjectFactory() { super(); }
    
    public PrototypeObjectFactory(String objectClassName) { super(objectClassName); }

    public PrototypeObjectFactory(String objectClassName, Map properties) { super(objectClassName, properties); }

    public PrototypeObjectFactory(Class objectClass) { super(objectClass); }

    public PrototypeObjectFactory(Class objectClass, Map properties) { super(objectClass, properties); }
    
    public void initialise() throws InitialisationException
    {
        instances = new ConcurrentHashMap();        
    }
    
    public void dispose()
    {
        if (instances != null)
        {
            Iterator it = instances.values().iterator();
            while (it.hasNext())
            {
                Object obj = it.next();
                if (obj instanceof Disposable)
                {
                    ((Disposable) obj).dispose();
                }
            }
            instances.clear();
            instances = null;
        }
    }

    /**
     * Creates a new instance of the object on each call.
     */
    public Object getOrCreate() throws Exception
    {
        Object obj = super.getOrCreate();
        String id = UUID.getUUID();
        if (obj instanceof Identifiable)
        {
            ((Identifiable) obj).setId(id);
        }
        if (instances != null)
        {
            instances.put(id, obj);
        }
        else 
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Object factory has not been initialized."), this);
        }
        return obj;
    }

    /** {@inheritDoc} */
    public Object lookup(String id) throws Exception
    {
        if (instances != null)
        {
            return instances.get(id);
        }
        else 
        {
            return null;
        }
    }

    /** 
     * Removes the object instance from the list of active instances.
     */
    public void release(Object object) throws Exception
    {
        if (object instanceof Identifiable)
        {
            instances.remove(((Identifiable) object).getId());
        }
        if (object instanceof Disposable)
        {
            ((Disposable) object).dispose();
        }
    }
}
