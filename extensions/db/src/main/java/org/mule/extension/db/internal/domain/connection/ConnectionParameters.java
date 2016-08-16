/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;

import javax.sql.DataSource;

public class ConnectionParameters {

  /**
   * Reference to a JDBC DataSource object. This object is typically created using Spring.
   * When using XA transactions, an XADataSource object must be provided.
   */
  @Parameter
  @Optional
  private DataSource dataSource;

  @ParameterGroup
  private DataSourceConfig dataSourceConfig;

  public DataSource getDataSource() {
    return dataSource;
  }

  public DataSourceConfig getDataSourceConfig() {
    return dataSourceConfig;
  }
}
