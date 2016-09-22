/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.mysql;

import org.mule.extension.db.api.config.MySqlDataSourceConfig;
import org.mule.extension.db.api.config.DataSourceConfig;
import org.mule.extension.db.api.config.DatabaseUrlConfig;
import org.mule.extension.db.internal.domain.connection.DbConnectionParameters;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import static org.mule.extension.db.internal.domain.connection.DbConnectionUtils.enrichWithDriverClass;

/**
 * Parameter group of exclusive connection parameters for MySql Databases.
 *
 * @since 4.0
 */
@ExclusiveOptionals(isOneRequired = true)
public class MySqlConnectionParameters implements DbConnectionParameters {

  private static final String MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver";

  @Optional
  @Parameter
  @Alias("mySqlParameters")
  private MySqlDataSourceConfig mySqlParameters;

  @Optional
  @Parameter
  private DatabaseUrlConfig databaseUrl;

  @Override
  public java.util.Optional<DataSourceConfig> getDataSourceConfig() {
    if (databaseUrl != null) {
      enrichWithDriverClass(databaseUrl, MYSQL_DRIVER_CLASS);
      return java.util.Optional.of(databaseUrl);
    }
    if (mySqlParameters != null) {
      return java.util.Optional.of(mySqlParameters);
    }
    return java.util.Optional.empty();
  }
}
