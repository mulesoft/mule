/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.extension.db.api.param.TransactionIsolation.NOT_CONFIGURED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.db.api.config.DbPoolingProfile;
import org.mule.extension.db.api.param.TransactionIsolation;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Password;

import java.util.concurrent.TimeUnit;

/**
 * Maintains configuration information about how to build a {@link javax.sql.DataSource}
 */
public class DataSourceConfig {

  /**
   * URL used to connect to the database.
   */
  @Parameter
  @Optional
  private String url;

  /**
   * Fully-qualified name of the database driver class.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private String driverClassName;


  /**
   * Maximum time that the data source will wait while attempting to connect to a
   * database. A value of zero (default) specifies that the timeout is the default system timeout if there is one;
   * otherwise, it specifies that there is no timeout.
   */
  @Parameter
  @Optional(defaultValue = "0")
  @Expression(NOT_SUPPORTED)
  private Integer connectionTimeout;


  /**
   * A {@link TimeUnit} which qualifies the {@link #connectionTimeout}
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Expression(NOT_SUPPORTED)
  private TimeUnit connectionTimeoutUnit = SECONDS;

  /**
   * The user that is used for authentication against the database
   */
  @Parameter
  @Optional
  private String user;

  /**
   * The password that is used for authentication against the database.
   */
  @Parameter
  @Optional
  @Password
  private String password;

  /**
   * The transaction isolation level to set on the driver when connecting the database.
   */
  @Parameter
  @Optional(defaultValue = "NOT_CONFIGURED")
  @Expression(NOT_SUPPORTED)
  private TransactionIsolation transactionIsolation = NOT_CONFIGURED;

  /**
   * Indicates whether or not the created datasource has to support XA transactions. Default is false.
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Expression(NOT_SUPPORTED)
  private boolean useXaTransactions = false;

  /**
   * Provides a way to configure database connection pooling.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private DbPoolingProfile poolingProfile;


  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDriverClassName() {
    return driverClassName;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public TimeUnit getConnectionTimeoutUnit() {
    return connectionTimeoutUnit;
  }

  public String getPassword() {
    return password;
  }

  public String getUser() {
    return user;
  }

  public TransactionIsolation getTransactionIsolation() {
    return transactionIsolation;
  }

  public boolean isUseXaTransactions() {
    return useXaTransactions;
  }

  public DbPoolingProfile getPoolingProfile() {
    return poolingProfile;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public void setConnectionTimeoutUnit(TimeUnit connectionTimeoutUnit) {
    this.connectionTimeoutUnit = connectionTimeoutUnit;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setTransactionIsolation(TransactionIsolation transactionIsolation) {
    this.transactionIsolation = transactionIsolation;
  }

  public void setUseXaTransactions(boolean useXaTransactions) {
    this.useXaTransactions = useXaTransactions;
  }

  public void setPoolingProfile(DbPoolingProfile poolingProfile) {
    this.poolingProfile = poolingProfile;
  }
}
