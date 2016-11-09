/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.connection;

import org.mule.extension.db.api.config.DbPoolingProfile;
import org.mule.extension.db.internal.domain.xa.CompositeDataSourceDecorator;
import org.mule.runtime.api.tx.DataSourceDecorator;
import org.mule.runtime.api.config.DatabasePoolingProfile;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.util.concurrent.ConcurrentHashSet;

import com.mchange.v2.c3p0.DataSources;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.enhydra.jdbc.standard.StandardDataSource;
import org.enhydra.jdbc.standard.StandardXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link DataSource} instances
 */
public class DataSourceFactory implements Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);

  private final String name;
  private final Set<DataSource> pooledDataSources = new ConcurrentHashSet();
  private final Set<Disposable> disposableDataSources = new ConcurrentHashSet();
  private final CompositeDataSourceDecorator dataSourceDecorator;

  public DataSourceFactory(String name, MuleContext muleContext) {
    this.name = name;
    dataSourceDecorator = new CompositeDataSourceDecorator(muleContext.getRegistry().lookupObjects(DataSourceDecorator.class));
  }

  /**
   * Creates a dataSource from a given dataSource config
   *
   * @param dataSourceConfig describes how to create the dataSource
   * @return a non null dataSource
   * @throws SQLException in case there is a problem creating the dataSource
   */
  public DataSource create(DataSourceConfig dataSourceConfig, DbPoolingProfile poolingProfile) throws SQLException {
    DataSource dataSource;

    if (poolingProfile == null) {
      dataSource = createSingleDataSource(dataSourceConfig);
    } else {
      dataSource = createPooledDataSource(dataSourceConfig, poolingProfile);
    }

    if (dataSourceConfig.isUseXaTransactions()) {
      dataSource = decorateDataSource(dataSource, poolingProfile);
    }

    if (!(poolingProfile == null || dataSourceConfig.isUseXaTransactions())) {
      pooledDataSources.add(dataSource);
    } else if (dataSource instanceof Disposable) {
      disposableDataSources.add((Disposable) dataSource);
    }

    return dataSource;
  }

  public DataSource decorateDataSource(DataSource dataSource, DatabasePoolingProfile poolingProfile) {
    return dataSourceDecorator.decorate(dataSource, name, poolingProfile);
  }

  protected DataSource createSingleDataSource(DataSourceConfig dataSourceConfig) throws SQLException {
    StandardDataSource dataSource =
        dataSourceConfig.isUseXaTransactions() ? new StandardXADataSource() : new StandardDataSource();
    dataSource.setDriverName(dataSourceConfig.getDriverClassName());
    dataSource.setPassword(dataSourceConfig.getPassword());
    dataSource.setTransactionIsolation(dataSourceConfig.getTransactionIsolation().getCode());
    dataSource.setUrl(dataSourceConfig.getUrl());
    dataSource.setUser(dataSourceConfig.getUser());

    return dataSource;
  }

  protected DataSource createPooledDataSource(DataSourceConfig dataSourceConfig, DbPoolingProfile poolingProfile)
      throws SQLException {
    if (dataSourceConfig.isUseXaTransactions()) {
      return createSingleDataSource(dataSourceConfig);
    } else {
      return createPooledStandardDataSource(createSingleDataSource(dataSourceConfig), poolingProfile);
    }
  }

  protected DataSource createPooledStandardDataSource(DataSource dataSource, DatabasePoolingProfile poolingProfile)
      throws SQLException {
    Map<String, Object> config = new HashMap<>();
    config.put("maxPoolSize", poolingProfile.getMaxPoolSize());
    config.put("minPoolSize", poolingProfile.getMinPoolSize());
    config.put("initialPoolSize", poolingProfile.getMinPoolSize());
    config.put("checkoutTimeout", new Long(poolingProfile.getMaxWaitUnit().toMillis(poolingProfile.getMaxWait())).intValue());
    config.put("acquireIncrement", poolingProfile.getAcquireIncrement());
    config.put("maxStatements", 0);
    config.put("testConnectionOnCheckout", "true");
    config.put("maxStatementsPerConnection", poolingProfile.getPreparedStatementCacheSize());

    return DataSources.pooledDataSource(dataSource, config);
  }

  @Override
  public void dispose() {
    for (DataSource pooledDataSource : pooledDataSources) {
      try {
        DataSources.destroy(pooledDataSource);
      } catch (SQLException e) {
        LOGGER.warn("Unable to properly release pooled data source", e);
      }
    }

    for (Disposable disposableDataSource : disposableDataSources) {
      try {
        disposableDataSource.dispose();
      } catch (Exception e) {
        LOGGER.warn("Unable to properly dispose data source", e);
      }
    }
  }
}
