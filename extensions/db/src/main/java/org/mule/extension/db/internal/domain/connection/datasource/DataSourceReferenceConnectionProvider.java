/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.datasource;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.util.Optional;

import javax.sql.DataSource;

/**
 * {@link ConnectionProvider} implementation which creates DB connections from a referenced {@link
 * DataSource}
 *
 * @since 4.0
 */
@DisplayName("Data Source Reference Connection")
@Alias("data-source")
public class DataSourceReferenceConnectionProvider extends DbConnectionProvider {

  @ParameterGroup(name = CONNECTION)
  private DataSourceConnectionSettings connectionSettings;

  @Override
  public Optional<DataSource> getDataSource() {
    return ofNullable(connectionSettings.getDataSourceRef());
  }

  @Override
  public Optional<DataSourceConfig> getDataSourceConfig() {
    return empty();
  }
}
