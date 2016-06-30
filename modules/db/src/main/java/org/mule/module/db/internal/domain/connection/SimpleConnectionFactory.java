/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import java.sql.Connection;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a {@link Connection} from a {@link DataSource}
 */
public class SimpleConnectionFactory extends AbstractConnectionFactory
{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<String, Class<?>> typeMapping;

    public SimpleConnectionFactory(Map<String, Class<?>> typeMapping)
    {
        this.typeMapping = typeMapping;
    }

    @Override
    protected Connection doCreateConnection(DataSource dataSource)
    {
        Connection connection;
        try
        {
            connection = dataSource.getConnection();

            if (typeMapping != null && !typeMapping.isEmpty())
            {
                try
                {
                    connection.setTypeMap(typeMapping);
                }
                catch (SQLFeatureNotSupportedException e)
                {
                    logger.warn("DataSource does not support custom type mappings - " + dataSource);
                }
            }
        }
        catch (Exception e)
        {
            throw new ConnectionCreationException(e);
        }

        return connection;
    }
}
