/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.xa;

import org.mule.api.MuleContext;

import java.util.Collection;
import java.util.LinkedList;

import javax.sql.DataSource;

public class CompositeDataSourceDecorator implements DataSourceDecorator
{

    private LinkedList<DataSourceDecorator> decorators = new LinkedList<DataSourceDecorator>();

    public CompositeDataSourceDecorator()
    {
        decorators.add(new DefaultDataSourceDecorator());
    }

    @Override
    public DataSource decorate(DataSource dataSource, String dataSourceName, MuleContext muleContext)
    {
        for (DataSourceDecorator decorator : decorators)
        {
            if (decorator.appliesTo(dataSource, muleContext))
            {
                return decorator.decorate(dataSource, dataSourceName, muleContext);
            }
        }
        return dataSource;
    }

    @Override
    public boolean appliesTo(DataSource dataSource, MuleContext muleContext)
    {
        return true;
    }

    public void init(MuleContext muleContext)
    {
        Collection<DataSourceDecorator> connectionFactoryDecorators = muleContext.getRegistry().lookupObjects(DataSourceDecorator.class);
        for (DataSourceDecorator connectionFactoryDecorator : connectionFactoryDecorators)
        {
            decorators.addFirst(connectionFactoryDecorator);
        }
    }
}
