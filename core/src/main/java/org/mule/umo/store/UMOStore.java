/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.store;

import org.mule.umo.lifecycle.Disposable;

/**
 * represents a store that any object can write state to. Stores can be
 * a database a cache, a repository, in memory or file
 */
public interface UMOStore extends Disposable
{
    public Object getObject(Object query) throws StoreException;

    public Object[] getObjects(Object query) throws StoreException;

    public void writeObject(Object object) throws StoreException;

    public String getName();

}
