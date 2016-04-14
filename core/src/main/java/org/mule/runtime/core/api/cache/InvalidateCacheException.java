/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.cache;

/**
 * Thrown to indicate an error during a cache invalidation action
 */
public class InvalidateCacheException extends RuntimeException
{

    public InvalidateCacheException(String s)
    {
        super(s);
    }

    public InvalidateCacheException(String s, Throwable throwable)
    {
        super(s, throwable);
    }
}
