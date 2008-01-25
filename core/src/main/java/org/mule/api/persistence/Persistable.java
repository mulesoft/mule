/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.persistence;

import org.mule.api.MuleException;

/**
 * <code>Persistable</code> is an interface for a Mule object that wants
 * to be persisted somewhere. Right now, this interface is envisioned for
 * the registry itself, but it can be extended to other components.
 * 
 * @author 
 * @version $Revision$
 */
public interface Persistable
{
    /**
     * Registers a listener from the Persistence Manager. This is
     * used by the service to alert the Manager to queue up the
     * persistence request.
     *
     * @param listener The listener that the service will talk to
     * @throws MuleException if the registration fails
     */
    void registerPersistenceRequestListener(PersistenceNotificationListener listener) throws MuleException;

    /**
     * Return the data for persistence. It is the responsibility
     * of the service to handle any locks, copies, deep copies,
     * or synchronizations necessary in order to preserve a consistent
     * backup.
     *
     * @returns the data to be persisted
     * @throws MuleException if the registration fails
     */

    Object getPersistableObject() throws MuleException;

    /**
     * Returns the storage key for this object. This can be a primative
     * or a String key (like a path-like String).
     *
     * This method will be called by the PersistenceManager in order
     * to determine whether/how to update the PersistenceStore
     */
    Object getStorageKey() throws MuleException;

    /**
     * Returns an optional PersistenceHelper to do any pre-persistence
     * chores on the part of this persistable object.
     */
    PersistenceHelper getPersistenceHelper();
}

