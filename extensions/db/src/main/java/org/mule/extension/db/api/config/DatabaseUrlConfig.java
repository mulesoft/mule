/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.config;

import org.mule.extension.db.api.param.TransactionIsolation;
import org.mule.extension.db.internal.domain.connection.AdvancedDbParameters;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.CONNECTION;

/**
 * {@link DataSourceConfig} to build {@link DataSource} from a plain URL and their Driver Class Name
 *
 * @since 4.0
 */
public final class DatabaseUrlConfig implements DataSourceConfig {

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
  @Optional
  @Placement(group = ADVANCED, order = 1)
  private String driverClassName;

  @ParameterGroup
  private AdvancedDbParameters advancedDbParameters;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUrl() {
    return url;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDriverClassName() {
    return driverClassName;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPassword() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUser() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransactionIsolation getTransactionIsolation() {
    return advancedDbParameters.getTransactionIsolation();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isUseXaTransactions() {
    return advancedDbParameters.isUseXaTransactions();
  }
}
