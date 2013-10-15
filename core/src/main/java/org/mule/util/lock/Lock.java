/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.lock;

import org.mule.api.lifecycle.Disposable;

/**
 * Interface to provide a locking mechanism to use in mule components
 */
public interface Lock<T> extends Disposable
{

    /*
     * Gets a lock over the resource identified with lockId
     */
    void lock(T lockId);

    /*
     * Releases lock over the resource identified with lockId
     */
    void unlock(T lockId);
    
}
