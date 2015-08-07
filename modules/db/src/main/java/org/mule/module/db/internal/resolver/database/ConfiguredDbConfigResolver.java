/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.database.DbConfig;

/**
 * Resolves a {@link DbConfig} delegating to the {@link DbConfigResolver}
 * corresponding to the configured config.
 */
public class ConfiguredDbConfigResolver extends AbstractDbConfigResolver
{

    private final DbConfigResolver dbConfigResolver;

    public ConfiguredDbConfigResolver(DbConfigResolver dbConfigResolver)
    {
        this.dbConfigResolver = dbConfigResolver;
    }

    @Override
    public DbConfig resolve(MuleEvent muleEvent) throws UnresolvableDbConfigException
    {
        return dbConfigResolver.resolve(muleEvent);
    }

    @Override
    protected DbConfig resolveDefaultConfig()
    {
        return dbConfigResolver.resolve(null);
    }
}
