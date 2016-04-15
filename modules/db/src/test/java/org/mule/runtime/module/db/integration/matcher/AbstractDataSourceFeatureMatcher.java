/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.matcher;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.internal.matchers.TypeSafeMatcher;

/**
 * Provides a base implementation for {@link DataSource} matchers
 */
public abstract class AbstractDataSourceFeatureMatcher extends TypeSafeMatcher<DataSource>
{

    @Override
    public boolean matchesSafely(DataSource dataSource)
    {
        boolean supportFeature = false;

        Connection connection = null;
        try
        {
            try
            {
                connection = dataSource.getConnection();
                DatabaseMetaData metaData;
                metaData = connection.getMetaData();
                try
                {
                    supportFeature = supportsFeature(metaData);
                }
                catch (Throwable t)
                {
                    // Ignore
                }
            }
            finally
            {
                if (connection != null)
                {
                    connection.close();
                }
            }
        }
        catch (Exception e)
        {
            // Ignore
        }

        return supportFeature;
    }

    protected abstract boolean supportsFeature(DatabaseMetaData metaData) throws SQLException;
}
