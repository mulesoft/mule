/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.db.internal.domain.xa;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.Preconditions;
import org.mule.runtime.module.db.internal.domain.connection.DbPoolingProfile;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorates a {@link DataSource} using a {@link DataSourceWrapper} if required
 */
public class DefaultDataSourceDecorator implements DataSourceDecorator {

  private static final Logger logger = LoggerFactory.getLogger(DefaultDataSourceDecorator.class);

  @Override
  public DataSource decorate(DataSource dataSource, String dataSourceName, DbPoolingProfile dbPoolingProfile,
                             MuleContext muleContext) {
    Preconditions.checkState(appliesTo(dataSource, muleContext),
                             "DefaultDataSourceDecorator cannot be applied to data source " + dataSource);
    if (dbPoolingProfile != null) {
      logger.warn("Pooling profile configuration cannot be used with current transaction manager and XADataSource");
    }
    return new DataSourceWrapper((XADataSource) dataSource);
  }

  @Override
  public boolean appliesTo(DataSource dataSource, MuleContext muleContext) {
    return !isDataSourceWrapper(dataSource) && isXaDataSource(dataSource);
  }

  private boolean isDataSourceWrapper(DataSource dataSource) {
    return (dataSource instanceof DataSourceWrapper);
  }

  private boolean isXaDataSource(DataSource dataSource) {
    return (dataSource instanceof XADataSource);
  }
}
