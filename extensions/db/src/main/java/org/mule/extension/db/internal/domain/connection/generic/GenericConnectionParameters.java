/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.generic;

import org.mule.extension.db.internal.domain.connection.DataSourceConfig;
import org.mule.extension.db.internal.domain.connection.BaseDbConnectionParameters;
import org.mule.extension.db.internal.domain.connection.DbConnectionParameters;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.CONNECTION;

/**
 *  {@link DbConnectionParameters} for the {@link GenericConnectionProvider}
 *
 * @since 4.0
 */
public final class GenericConnectionParameters extends BaseDbConnectionParameters implements DataSourceConfig {

  /**
   * JDBC URL to be used to connect to the database.
   */
  @Parameter
  @Placement(group = CONNECTION, order = 1)
  @DisplayName("URL")
  private String url;

  /**
   * Fully-qualified name of the database driver class.
   */
  @Parameter
  @Placement(group = CONNECTION, order = 2)
  private String driverClassName;

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String getDriverClassName() {
    return driverClassName;
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUser() {
    return null;
  }
}
