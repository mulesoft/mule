/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import org.mule.AbstractAnnotatedObject;
import org.mule.common.Result;
import org.mule.common.TestResult;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.module.db.internal.domain.database.DbConfig;

import java.util.List;

/**
 * Provides a base implementation for resolver's metadata related functionality
 */
public abstract class AbstractDbConfigResolver extends AbstractAnnotatedObject implements DbConfigResolver
{

    @Override
    public TestResult test()
    {
        DbConfig dbConfig = resolveDefaultConfig();

        return dbConfig.test();
    }

    @Override
    public Result<List<MetaDataKey>> getMetaDataKeys()
    {
        DbConfig dbConfig = resolveDefaultConfig();

        return dbConfig.getMetaDataKeys();
    }

    @Override
    public Result<MetaData> getMetaData(MetaDataKey metaDataKey)
    {
        DbConfig dbConfig = resolveDefaultConfig();

        return dbConfig.getMetaData(metaDataKey);
    }

    protected DbConfig resolveDefaultConfig()
    {
        return resolve(null);
    }
}
