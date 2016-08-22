/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.mysql;

import org.mule.extension.db.internal.domain.connection.AbstractVendorConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Creates connections to a MySQL database.
 *
 * Connection can be specified through a URL, or through
 * convenience parameters exposed to spare the user from
 * the need of knowing the specific URL format.
 * <p>
 * Notice those parameters are ignored if a specific URL
 * is provided.
 *
 * @since 4.0
 */
@DisplayName("MySQL Connection")
@Alias("my-sql")
public class MySqlConnectionProvider extends AbstractVendorConnectionProvider {

  @Override
  protected DataSource createDataSource() throws SQLException {
    connectionParameters.getDataSourceConfig().setDriverClassName("com.mysql.jdbc.Driver");
    return super.createDataSource();
  }

  @Override
  protected String getUrlPrefix() {
    return "jdbc:mysql://";
  }
}
