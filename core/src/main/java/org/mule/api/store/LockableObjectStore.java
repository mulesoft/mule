/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.store;

import java.io.Serializable;

public interface LockableObjectStore<T extends Serializable> extends ObjectStore<T>
{
    /**
     * Locks a key in the store so no other thread can read/update this key until is released.
     */
    void lockEntry(Serializable key);

    /**
     * Unlock a key in the store so other threads can access the entry associated with the key.
     */
    void releaseEntry(Serializable key);

}
