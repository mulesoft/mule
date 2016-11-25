/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.mysql;

import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.util.Optional;

import javax.sql.DataSource;

/**
 * Creates connections to a MySQL database.
 *
 * @since 4.0
 */
@DisplayName("MySQL Connection")
@Alias("my-sql")
public class MySqlConnectionProvider extends DbConnectionProvider {

  @ParameterGroup(CONNECTION)
  private MySqlConnectionParameters mySqlParameters;

  @Override
  public Optional<DataSource> getDataSource() {
    return Optional.empty();
  }

  @Override
  public Optional<DataSourceConfig> getDataSourceConfig() {
    return Optional.ofNullable(mySqlParameters);
  }
}
