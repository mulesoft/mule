/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Builds {@link DatabaseMetaData} mocks
 */
public class DatabaseMetaDataBuilder
{

    private final DatabaseMetaData databaseMetaData;

    public DatabaseMetaDataBuilder()
    {
        databaseMetaData = mock(DatabaseMetaData.class);
    }

    public DatabaseMetaDataBuilder returningStoredProcedureColumns(String catalog, String storedProcedureName, ResultSet procedureColumns)
    {
        try
        {
            when(databaseMetaData.getProcedureColumns(catalog, null, storedProcedureName, "%")).thenReturn(procedureColumns);
        }
        catch (SQLException e)
        {
            // Not going to happen when building the mock
        }

        return this;
    }

    public DatabaseMetaData build()
    {
        return databaseMetaData;
    }
}
