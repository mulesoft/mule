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
import org.mule.api.lifecycle.Initialisable;

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
    Object getInstance() throws Exception;
    
    /**
     * Returns the class of the object to be instantiated without actually creating an
     * instance.  This may not be logical or even possible depending on the implementation.
     */
    Class getObjectClass() throws Exception;

    /**
     * Inform the object factory/container that this object is no longer in use.  
     * This may return the object to a pool, deallocate resources, or do something
     * else depending on the implementation.  If appropriate, the object will be disposed
     * by this method (Disposable.dispose()).
     */
    void release(Object object) throws Exception;
}
