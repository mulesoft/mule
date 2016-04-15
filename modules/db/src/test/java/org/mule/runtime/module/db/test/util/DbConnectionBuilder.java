/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.connection.DbConnection;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Builds {@link DbConnection} mocks
 */
public class DbConnectionBuilder
{

    private final DbConnection connection;

    public DbConnectionBuilder()
    {
        connection = mock(DbConnection.class);
    }

    public DbConnectionBuilder onCalatog(String name)
    {
        try
        {
            when(connection.getCatalog()).thenReturn(name);
        }
        catch (SQLException e)
        {
            // Not going to happen when building the mock
        }

        return this;
    }

    public DbConnectionBuilder with(DatabaseMetaData metaData)
    {
        try
        {
            when(connection.getMetaData()).thenReturn(metaData);
        }
        catch (SQLException e)
        {
            // Not going to happen when building the mock
        }

        return this;
    }

    public DbConnectionBuilder preparing(String sqlText, PreparedStatement statement)
    {
        try
        {
            when(connection.prepareStatement(sqlText)).thenReturn(statement);
        }
        catch (SQLException e)
        {
            // Not going to happen when building the mock
        }

        return this;
    }


    public DbConnection build()
    {
        return connection;
    }
}
