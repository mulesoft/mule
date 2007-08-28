/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence;

import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;

/**
 * <code>PersistenceStore</code> is the actual store where persisted
 * objects are stored. It is owned by the PersistenceManager.
 */
public interface PersistenceStore extends Initialisable, Disposable
{
    /**
     * Asks the store to store a Persistable object. 
     *
     * @object object to persist
     * @myUpdate whether the store has the option to update or not
     */
    void store(Persistable object, boolean mayUpdate) throws PersistenceException;

    /**
     * Asks the store to remove a Persistable object.
     *
     * @object object to be removed
     */
    void remove(Persistable object) throws PersistenceException;

    /**
     * Tells whether the store is ready for persistence or not
     */
    boolean isReady();

    /**
     * Returns the current state of the store
     */
    int getState();
}

