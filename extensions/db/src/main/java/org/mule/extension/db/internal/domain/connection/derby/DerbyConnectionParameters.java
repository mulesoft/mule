/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;

import org.mule.extension.db.api.config.DerbyDataSourceConfig;
import org.mule.extension.db.api.config.DataSourceConfig;
import org.mule.extension.db.api.config.DatabaseUrlConfig;
import org.mule.extension.db.internal.domain.connection.DbConnectionParameters;
import org.mule.runtime.extension.api.annotation.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import static org.mule.extension.db.internal.domain.connection.DbConnectionUtils.enrichWithDriverClass;

/**
 * Parameter group of exclusive connection parameters for Derby Databases.
 *
 * @since 4.0
 */
@ExclusiveOptionals(isOneRequired = true)
public class DerbyConnectionParameters implements DbConnectionParameters {

  private static final String DERBY_DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";

  /**
   * {@link DataSourceConfig} that lets you connect to the Derby database using a JDBC URL
   */
  @Optional
  @Parameter
  private DatabaseUrlConfig databaseUrl;

  /**
   * {@link DataSourceConfig} specialization for Derby databases
   */
  @Optional
  @Parameter
  private DerbyDataSourceConfig derbyParameters;

  @Override
  public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
    if (databaseUrl != null) {
      enrichWithDriverClass(databaseUrl, DERBY_DRIVER_CLASS);
      return java.util.Optional.of(databaseUrl);
    }
    if (derbyParameters != null) {
      return java.util.Optional.of(derbyParameters);
    }
    return java.util.Optional.empty();
  }
}
