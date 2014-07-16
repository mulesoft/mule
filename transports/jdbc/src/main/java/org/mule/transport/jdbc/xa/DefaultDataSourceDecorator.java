/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.xa;

import org.mule.api.MuleContext;
import org.mule.util.Preconditions;

import javax.sql.DataSource;
import javax.sql.XADataSource;

public class DefaultDataSourceDecorator implements DataSourceDecorator
{

    @Override
    public DataSource decorate(DataSource dataSource, String dataSourceName, MuleContext muleContext)
    {
        Preconditions.checkState(appliesTo(dataSource, muleContext), "DefaultDataSourceDecorator cannot be applied to data source " + dataSource);
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
