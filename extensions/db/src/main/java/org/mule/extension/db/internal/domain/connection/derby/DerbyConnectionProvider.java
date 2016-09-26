/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;

import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.util.Optional;

import javax.sql.DataSource;

/**
 * Creates connections to a Derby database
 *
 * @since 4.0
 */
@DisplayName("Derby Connection")
@Alias("derby")
public class DerbyConnectionProvider extends DbConnectionProvider {

  @ParameterGroup
  private DerbyConnectionParameters derbyParameters;

  @Override
  public Optional<DataSource> getDataSource() {
    return Optional.empty();
  }

  @Override
  public Optional<DataSourceConfig> getDataSourceConfig() {
    return Optional.ofNullable(derbyParameters);
  }
}
