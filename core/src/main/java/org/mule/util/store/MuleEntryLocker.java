/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

/**
 *
 * Provides a lock mechanism for share information in Mule.
 *
 */
public interface MuleEntryLocker<T>
{
    /**
     * Creates a lock around a lockIdentifier.
     * To release lock use release method with the same identifier
     */
    public void lock(T lockIdentifier);

    /**
     *  Releases a lock previously locked.
     */
    public void release(T lockIdentifier);
}
