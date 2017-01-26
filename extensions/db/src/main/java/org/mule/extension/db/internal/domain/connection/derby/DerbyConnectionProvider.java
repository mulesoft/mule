/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.extension.db.api.exception.connection.DbError.CANNOT_REACH;
import static org.mule.extension.db.internal.domain.connection.DbConnectionProvider.DRIVER_FILE_NAME_PATTERN;
import static org.mule.extension.db.internal.domain.connection.derby.DerbyConnectionParameters.DERBY_DRIVER_CLASS;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import org.mule.extension.db.api.exception.connection.ConnectionCreationException;
import org.mule.extension.db.api.exception.connection.DbError;
import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Creates connections to a Derby database
 *
 * @since 4.0
 */
@DisplayName("Derby Connection")
@Alias("derby")
@ExternalLib(name = "Derby JDBC Driver", description = "A JDBC driver which supports connecting to a Derby Database",
    fileName = DRIVER_FILE_NAME_PATTERN, requiredClassName = DERBY_DRIVER_CLASS)
public class DerbyConnectionProvider extends DbConnectionProvider {

  private static final String FAILED_TO_START_DATABASE = "Failed to start database";
  private static final String NOT_FOUND = "not found";

  @ParameterGroup(name = CONNECTION)
  private DerbyConnectionParameters derbyParameters;

  @Override
  public Optional<DataSource> getDataSource() {
    return empty();
  }

  @Override
  public Optional<DataSourceConfig> getDataSourceConfig() {
    return ofNullable(derbyParameters);
  }

  @Override
  public Optional<DbError> getDbErrorType(SQLException e) {
    if (e.getMessage().contains(FAILED_TO_START_DATABASE)) {
      return Optional.of(CANNOT_REACH);
    } else if (e.getMessage().contains(NOT_FOUND)) {
      return Optional.of(CANNOT_REACH);
    }
    return empty();
  }
}
