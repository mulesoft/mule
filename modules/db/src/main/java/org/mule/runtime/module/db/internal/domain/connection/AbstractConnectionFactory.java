/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import java.sql.Connection;

import javax.sql.DataSource;

/**
 * Base class for {@link ConnectionFactory}
 */
public abstract class AbstractConnectionFactory implements ConnectionFactory
{

    @Override
    public final Connection create(DataSource dataSource)
    {
        Connection connection = doCreateConnection(dataSource);

        if (connection == null)
        {
          throw new ConnectionCreationException("Unable to create connection to the provided dataSource: " + dataSource);
        }

        return connection;
    }

    protected abstract Connection doCreateConnection(DataSource dataSource);
}
