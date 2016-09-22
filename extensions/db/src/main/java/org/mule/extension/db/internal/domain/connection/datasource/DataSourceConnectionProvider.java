/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.datasource;

import org.mule.extension.db.internal.domain.connection.DbConnectionParameters;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.CONNECTION;

/**
 * {@link ConnectionProvider} implementation which creates DB connections from a referenced {@link
 * DataSource}
 *
 * @since 4.0
 */
@DisplayName("Data Source Connection")
@Alias("data-source")
public class DataSourceConnectionProvider extends DbConnectionProvider {

  /**
   * Reference to a JDBC {@link DataSource} object. This object is typically created using Spring.
   * When using XA transactions, an {@link XADataSource} object must be provided.
   */
  @Parameter
  @Alias("dataSource")
  @Placement(group = CONNECTION)
  //This, non optimal, name is due that this field clashes with the one defined in DbConnectionProvider
  private DataSource aDataSource;

  @Override
  public DbConnectionParameters getConnectionParameters() {
    return new DbConnectionParameters() {

      @Override
      public java.util.Optional<DataSource> getDataSource() {
        return java.util.Optional.of(aDataSource);
      }
    };
  }
}
