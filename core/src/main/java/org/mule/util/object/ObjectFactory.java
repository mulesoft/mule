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

/**
 * <code>ObjectFactory</code> is a generic Factory interface.
 * TODO MULE-2137 Rename to ObjectReference
 */
public interface ObjectFactory extends Initialisable, Disposable
{
    /**
     * Retrieve an instance of the object.  This may create a new instance or look up 
     * an existing instance depending on the implementation.  If a new instance
     * is created it will also be initialized by this method (Initilisable.initialise()).
     */
    Object getOrCreate() throws Exception;
    
    /**
     * Look up a previously created object instance by ID.
     */
    Object lookup(String id) throws Exception;

    /**
     * Inform the object factory/container that this object is no longer in use.  
     * This may return the object to a pool, deallocate resources, or do something
     * else depending on the implementation.  If appropriate, the object will be disposed
     * by this method (Disposable.dispose()).
     */
    void release(Object object) throws Exception;
}
