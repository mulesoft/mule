/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.matcher;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Description;

public class SupportsReturningStoredProcedureResultsWithoutParameters extends AbstractDataSourceFeatureMatcher
{

    private static final Set<String> supportedProducts;

    static
    {
       supportedProducts = new HashSet<String>();
       supportedProducts.add("MYSQL");
       supportedProducts.add("APACHE DERBY");
    }

    @Override
    protected boolean supportsFeature(DatabaseMetaData metaData) throws SQLException
    {
        String productName = metaData.getDatabaseProductName().toUpperCase();
        return supportedProducts.contains(productName);
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("Database only returns stored procedure results with output parameters");
    }
}
