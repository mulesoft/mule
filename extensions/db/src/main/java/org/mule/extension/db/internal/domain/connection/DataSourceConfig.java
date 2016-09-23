/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import org.mule.extension.db.api.param.TransactionIsolation;

import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

/**
 * Contract for DataSource Configurations that are used to build {@link DataSource} instances
 *
 * @since 4.0
 */
public interface DataSourceConfig {

  /**
   * The JDBC URL to be used to connect to the database
   */
  String getUrl();

  /**
   * Full qualifier name of the Driver Class to connect to the database
   */
  String getDriverClassName();

  /**
   * Password to use to login into the database
   */
  String getPassword();

  /**
   * User to use to login into the database
   */
  String getUser();

  /**
   * The transaction isolation level to set on the driver when connecting the database.
   */
  TransactionIsolation getTransactionIsolation();

  /**
   * Indicates whether or not the created datasource has to support XA transactions. Default is
   * false.
   */
  boolean isUseXaTransactions();
}
