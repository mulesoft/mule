/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

public class DefaultDbConnectionReleaser implements DbConnectionReleaser
{

    private final DbConnectionFactory dbConnectionFactory;

    public DefaultDbConnectionReleaser(DbConnectionFactory dbConnectionFactory)
    {
        this.dbConnectionFactory = dbConnectionFactory;
    }

    @Override
    public void release(DbConnection connection)
    {
        dbConnectionFactory.releaseConnection(connection);
    }
}
