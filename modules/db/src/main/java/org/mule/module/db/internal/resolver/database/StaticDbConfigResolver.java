/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.Disposable;
import org.mule.module.db.internal.domain.database.DbConfig;

import javax.sql.DataSource;

/**
 * Resolves a {@link DbConfig} to a static value without using the current event
 */
public class StaticDbConfigResolver extends AbstractDbConfigResolver implements Disposable
{

    private final DbConfig dbConfig;

    public StaticDbConfigResolver(DbConfig dbConfig)
    {
        this.dbConfig = dbConfig;
    }

    @Override
    public DbConfig resolve(MuleEvent muleEvent)
    {
        return dbConfig;
    }

    @Override
    public void dispose()
    {
        DataSource dataSource = dbConfig.getDataSource();
        if (dataSource instanceof Disposable)
        {
            ((Disposable) dataSource).dispose();
        }
    }
}
