/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.internal.domain.xa;

import org.mule.api.MuleContext;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;
import org.mule.util.Preconditions;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Decorates a {@link DataSource} using a {@link DataSourceWrapper} if required
 */
public class DefaultDataSourceDecorator implements DataSourceDecorator
{

    private static final Log logger = LogFactory.getLog(DefaultDataSourceDecorator.class);

    @Override
    public DataSource decorate(DataSource dataSource, String dataSourceName, DbPoolingProfile dbPoolingProfile, MuleContext muleContext)
    {
        Preconditions.checkState(appliesTo(dataSource, muleContext), "DefaultDataSourceDecorator cannot be applied to data source " + dataSource);
        if (dbPoolingProfile != null)
        {
            logger.warn("Pooling profile configuration cannot be used with current transaction manager and XADataSource");
        }
        return new DataSourceWrapper((XADataSource) dataSource);
    }

    @Override
    public boolean appliesTo(DataSource dataSource, MuleContext muleContext)
    {
        return !isDataSourceWrapper(dataSource) && isXaDataSource(dataSource);
    }

    private boolean isDataSourceWrapper(DataSource dataSource)
    {
        return (dataSource instanceof DataSourceWrapper);
    }

    private boolean isXaDataSource(DataSource dataSource)
    {
        return (dataSource instanceof XADataSource);
    }
}
