/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

/**
 * Thrown to indicate an error during query resolution
 */
public class QueryResolutionException extends RuntimeException
{

    public QueryResolutionException(String message)
    {
        super(message);
    }

    public QueryResolutionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
