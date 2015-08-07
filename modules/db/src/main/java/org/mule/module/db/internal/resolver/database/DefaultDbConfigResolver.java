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
public class DefaultDbConfigResolver extends AbstractDbConfigResolver
{

    private final MuleRegistry registry;

    private DbConfigResolver defaultConfigResolver;

    public DefaultDbConfigResolver(MuleRegistry registry)
    {
        this.registry = registry;
    }

    private DbConfigResolver getDefaultConfigResolver()
    {

        Collection<DbConfigResolver> dbConfigResolvers = registry.lookupObjects(DbConfigResolver.class);

        if (dbConfigResolvers.isEmpty())
        {
            throw new UnresolvableDbConfigException("There is no database config defined");
        }

        if (dbConfigResolvers.size() > 1)
        {
            StringBuilder stringBuilder = new StringBuilder();
            for (DbConfigResolver dbConfigResolver : dbConfigResolvers)
            {
                if (stringBuilder.length() != 0)
                {
                    stringBuilder.append(", ");
                }

                stringBuilder.append(dbConfigResolver.resolve(null).getName());
            }

            throw new UnresolvableDbConfigException("Database config must be explicitly defined using 'config-ref' attribute there are multiple database configs defined: " + stringBuilder);
        }

        return dbConfigResolvers.iterator().next();
    }

    @Override
    public DbConfig resolve(MuleEvent muleEvent)
    {
        if (defaultConfigResolver == null)
        {
            synchronized (this)
            {
                if (defaultConfigResolver == null)
                {
                    defaultConfigResolver = getDefaultConfigResolver();
                }
            }
        }

        return defaultConfigResolver.resolve(muleEvent);
    }
}
