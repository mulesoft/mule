/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.matcher;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hamcrest.Description;

/**
 * Checks whether or not a dataSource supports to have multiple ResultSet
 * objects returned from a CallableStatement object simultaneously.
 */
public class SupportMultipleOpenResults extends AbstractDataSourceFeatureMatcher
{

    @Override
    protected boolean supportsFeature(DatabaseMetaData metaData) throws SQLException
    {
        return metaData.supportsMultipleOpenResults();
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("database.supportMultipleOpenResults == true");
    }
}
