/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

/**
 * Thrown to indicates an error binding a connection to a transaction
 */
public class ConnectionBindingException extends RuntimeException
{

    public ConnectionBindingException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

}
