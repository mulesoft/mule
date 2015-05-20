/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.database.DataSourceConfig;
import org.mule.module.db.internal.domain.database.DataSourceFactory;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.database.DbConfigFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

/**
 * Resolves a {@link DbConfig} from a dynamic {@link DataSourceConfig}.
 * <p>
 * Resolved {@link DbConfig} are cached and reused every time a
 * configured is resolved to the same static {@link DataSourceConfig}
 * </p>
 */
public class DynamicDbConfigResolver implements DbConfigResolver
{

    private final String name;
    private final DbConfigFactory dbConfigFactory;
    private final DataSourceConfig dataSourceConfig;
    private final DataSourceFactory dataSourceFactory;
    private final Map<DataSourceConfig, DbConfig> cache = new HashMap<>();
    private int instanceCount = 1;

    public DynamicDbConfigResolver(String name, DbConfigFactory dbConfigFactory, DataSourceFactory dataSourceFactory, DataSourceConfig dataSourceConfig)
    {
        this.name = name;
        this.dbConfigFactory = dbConfigFactory;
        this.dataSourceConfig = dataSourceConfig;
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public DbConfig resolve(MuleEvent muleEvent) throws UnresolvableDbConfigException
    {
        DataSourceConfig resolvedDataSourceConfig = this.dataSourceConfig.resolve(muleEvent);

        DbConfig dbConfig = cache.get(resolvedDataSourceConfig);

        if (dbConfig == null)
        {
            synchronized (cache)
            {
                dbConfig = cache.get(resolvedDataSourceConfig);
                if (dbConfig == null)
                {
                    try
                    {
                        DataSource dynamicDataSource = dataSourceFactory.create(resolvedDataSourceConfig);
                        dbConfig = dbConfigFactory.create(generateDbConfigName(), dynamicDataSource);
                        cache.put(resolvedDataSourceConfig, dbConfig);
                    }
                    catch (SQLException e)
                    {
                        throw new UnresolvableDbConfigException("Cannot create dynamic dataSource", e);
                    }
                }
            }
        }

        return dbConfig;
    }

    private String generateDbConfigName()
    {
        return name + "-dynamic-" + instanceCount++;
    }
}
