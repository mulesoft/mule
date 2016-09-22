/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import org.mule.extension.db.api.config.DatabaseUrlConfig;

/**
 * Utility class for DB Connections
 *
 * @since 4.0
 */
public class DbConnectionUtils {

  public static void enrichWithDriverClass(DatabaseUrlConfig databaseUrl, String driverClass) {
    if (databaseUrl.getDriverClassName() == null) {
      databaseUrl.setDriverClassName(driverClass);
    }
  }
}
