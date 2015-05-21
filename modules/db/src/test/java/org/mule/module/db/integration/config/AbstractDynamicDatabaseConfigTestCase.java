/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import org.mule.api.MuleEvent;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;

public abstract class AbstractDynamicDatabaseConfigTestCase extends AbstractDatabaseConfigTestCase
{

    public AbstractDynamicDatabaseConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected DbConfig resolveConfig(DbConfigResolver dbConfigResolver)
    {
        try
        {
            MuleEvent muleEvent = getTestEvent(TEST_MESSAGE);

            return dbConfigResolver.resolve(muleEvent);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to create test event to resolve DbConfig");
        }
    }
}
