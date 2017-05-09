/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.connectivity.negative;

import static org.hamcrest.CoreMatchers.is;
import static org.mule.extension.db.api.exception.connection.DbError.CANNOT_LOAD_DRIVER;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

import org.junit.Test;

public class MissingDriverNegativeTestCase extends AbstractDbNegativeConnectivityTestCase {

  @Test
  public void oracleMissingDriver() {
    utils.assertFailedConnection("oracleConfigInvalidCredentials", IS_CONNECTION_EXCEPTION, is(errorType(CANNOT_LOAD_DRIVER)));
  }

  @Test
  public void genericDriverMissingClass() {
    utils.assertFailedConnection("driverClassNotFound", IS_CONNECTION_EXCEPTION, is(errorType(CANNOT_LOAD_DRIVER)));
  }
}
