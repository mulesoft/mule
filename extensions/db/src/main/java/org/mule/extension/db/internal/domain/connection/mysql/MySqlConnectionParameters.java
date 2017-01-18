/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.mysql;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.extension.db.internal.domain.connection.BaseDbConnectionParameters;
import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link DataSourceConfig} implementation for MySQL databases.
 *
 * @since 4.0
 */
public final class MySqlConnectionParameters extends BaseDbConnectionParameters implements DataSourceConfig {

  static final String MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver";
  private static final String MY_SQL_PREFIX = "jdbc:mysql://";

  /**
   * Configures the host of the database
   */
  @Parameter
  @Placement(order = 1)
  private String host;

  /**
   * Configures the port of the database
   */
  @Parameter
  @Placement(order = 2)
  private Integer port;

  /**
   * The user that is used for authentication against the database
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  private String user;

  /**
   * The password that is used for authentication against the database
   */
  @Parameter
  @Optional
  @Placement(order = 4)
  @Password
  private String password;

  /**
   * The name of the database
   */
  @Parameter
  @Optional
  @Placement(order = 5)
  private String database;

  /**
   * Specifies a list of custom key-value connectionProperties for the config.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED_TAB)
  private Map<String, String> connectionProperties = new HashMap<>();

  @Override
  public String getUrl() {
    return MySqlDbUtils.getEffectiveUrl(MY_SQL_PREFIX, host, port, database, connectionProperties);
  }

  @Override
  public String getDriverClassName() {
    return MYSQL_DRIVER_CLASS;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUser() {
    return user;
  }
}
