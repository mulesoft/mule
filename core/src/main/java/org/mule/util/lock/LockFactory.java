/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.lock;

/**
 * Factory for creating Lock instances.
 *
 * Default LockFactory can be override by modules using registry-bootstrap.
 */
public interface LockFactory<T>
{

    /**
     * Creates a Lock for a given resource using the resource unique identifier.
     */
    Lock<T> createLock(String lockResourceName);
    
}
