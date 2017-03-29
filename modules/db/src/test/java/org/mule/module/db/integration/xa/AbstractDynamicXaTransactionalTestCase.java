/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.xa;

import org.mule.api.MuleEvent;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.DerbyTestDatabase;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;

import java.util.Collections;
import java.util.List;

import org.junit.runners.Parameterized;

public abstract class AbstractDynamicXaTransactionalTestCase extends AbstractXaTransactionalTestCase
{

    public AbstractDynamicXaTransactionalTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return Collections.singletonList(new Object[] {"integration/config/dynamic-derby-xa-db-config.xml", new DerbyTestDatabase()});
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {getTransactionManagerResource(), "integration/xa/dynamic-xa-transactional-config.xml"};
    }

    @Override
    protected DbConfig resolveConfig(DbConfigResolver dbConfigResolver)
    {
        try
        {
            MuleEvent muleEvent = getTestEvent(TEST_MESSAGE);
            muleEvent.getMessage().setInvocationProperty("dataSourceUrl", "jdbc:derby:target/muleEmbeddedDB;create=true");

            return dbConfigResolver.resolve(muleEvent);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to create test event to resolve DbConfig");
        }
    }
}
