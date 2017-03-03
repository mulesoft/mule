/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

/**
 * Thrown to indicate an error creating a connection
 */
public class ConnectionCreationException extends RuntimeException
{

    public ConnectionCreationException(String message)
    {
        super(message);
    }

    public ConnectionCreationException(Throwable throwable)
    {
        super(throwable);
    }

    public ConnectionCreationException(Throwable throwable, String message)
    {
        super(message, throwable);
    }
}
