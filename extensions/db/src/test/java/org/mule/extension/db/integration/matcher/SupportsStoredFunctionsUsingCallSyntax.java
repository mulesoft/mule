/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.matcher;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hamcrest.Description;

/**
 * Checks whether or not a dataSource supports invoking user-defined or vendor functions using the stored procedure escape syntax.
 */
public class SupportsStoredFunctionsUsingCallSyntax extends AbstractDataSourceFeatureMatcher {

  @Override
  protected boolean supportsFeature(DatabaseMetaData metaData) throws SQLException {
    return metaData.supportsStoredFunctionsUsingCallSyntax();
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("database.supportsStoredFunctionsUsingCallSyntax == true");
  }
}
