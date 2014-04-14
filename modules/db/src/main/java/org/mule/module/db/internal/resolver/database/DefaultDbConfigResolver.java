/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import org.mule.api.MuleEvent;
import org.mule.api.registry.MuleRegistry;
import org.mule.module.db.internal.domain.database.DbConfig;

import java.util.Collection;

/**
 * Resolves the default database configuration defined in an application
 */
public class DefaultDbConfigResolver implements DbConfigResolver
{

    private final MuleRegistry registry;

    private DbConfig defaultConfig;

    public DefaultDbConfigResolver(MuleRegistry registry)
    {
        this.registry = registry;
    }

    private DbConfig getDefaultConfig()
    {

        Collection<DbConfig> dbConfigs = registry.lookupObjects(DbConfig.class);

        if (dbConfigs.size() == 0)
        {
            throw new UnresolvableDbConfigException("There is no database config defined");
        }

        if (dbConfigs.size() > 1)
        {
            StringBuilder stringBuilder = new StringBuilder();
            for (DbConfig dbConfig : dbConfigs)
            {
                if (stringBuilder.length() != 0)
                {
                    stringBuilder.append(", ");
                }

                stringBuilder.append(dbConfig.getName());
            }

            throw new UnresolvableDbConfigException("Database config must be explicitly defined using 'config-ref' attribute there are multiple database configs defined: " + stringBuilder);
        }

        return dbConfigs.iterator().next();
    }

    @Override
    public DbConfig resolve(MuleEvent muleEvent)
    {
        if (defaultConfig == null)
        {
            synchronized (this)
            {
                if (defaultConfig == null)
                {
                    defaultConfig = getDefaultConfig();
                }
            }
        }

        return defaultConfig;
    }
}
