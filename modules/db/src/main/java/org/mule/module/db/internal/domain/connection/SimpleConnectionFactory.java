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
 * Creates a {@link Connection} from a {@link DataSource}
 */
public class SimpleConnectionFactory implements ConnectionFactory
{

    @Override
    public Connection create(DataSource dataSource)
    {
        Connection connection;
        try
        {
            connection = dataSource.getConnection();
        }
        catch (Exception e)
        {
            throw new ConnectionCreationException(e);
        }

        return connection;
    }

}
