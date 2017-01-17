/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.oracle;

import static java.util.Optional.empty;
import static org.mule.extension.db.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.extension.db.internal.domain.connection.oracle.OracleConnectionParameters.DRIVER_CLASS_NAME;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.extension.db.internal.domain.connection.JdbcConnectionFactory;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.sql.Connection;
import java.util.Optional;

import javax.sql.DataSource;

/**
 * Creates connections to a Oracle database
 *
 * @since 4.0
 */
@DisplayName("Oracle Connection")
@Alias("oracle")
@ExternalLib(name = "Oracle JDBC Driver", description = "A JDBC driver which supports connecting to an Oracle Database",
    fileName = DRIVER_FILE_NAME_PATTERN, requiredClassName = DRIVER_CLASS_NAME)
public class OracleDbConnectionProvider extends DbConnectionProvider {

  @ParameterGroup(name = CONNECTION)
  private OracleConnectionParameters oracleConnectionParameters;

  @Override
  protected JdbcConnectionFactory createJdbcConnectionFactory() {
    return new OracleJdbcConnectionFactory();
  }

  @Override
  protected DbConnection createDbConnection(Connection connection) throws Exception {
    return new OracleDbConnection(connection, super.resolveCustomTypes());
  }

  @Override
  public Optional<DataSource> getDataSource() {
    return empty();
  }

  @Override
  public Optional<DataSourceConfig> getDataSourceConfig() {
    return Optional.ofNullable(oracleConnectionParameters);
  }
}
