/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import org.mule.module.db.internal.domain.database.DbConfig;

/**
 * Thrown to indicate that is not possible to resolve a {@link DbConfig}
 */
public class UnresolvableDbConfigException extends RuntimeException
{

    public UnresolvableDbConfigException(String message)
    {
        super(message);
    }

    public UnresolvableDbConfigException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
