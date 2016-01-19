/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.DerbyTestDatabase;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectStreamingExceptionTestCase extends AbstractDbIntegrationTestCase
{

    private static final int POOL_CONNECTIONS = 2;

    public SelectStreamingExceptionTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return Collections.singletonList(new Object[] {"integration/config/derby-pooling-db-config.xml", new DerbyTestDatabase()});
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/select/select-streaming-exception-config.xml"};
    }

    @Override
    protected DataSource getDefaultDataSource()
    {
        DbConfigResolver dbConfigResolver = muleContext.getRegistry().get("pooledJdbcConfig");
        DbConfig config = resolveConfig(dbConfigResolver);

        return config.getDataSource();
    }

    @Test
    public void streamingException() throws Exception
    {
        for (int i = 0; i < POOL_CONNECTIONS + 1; ++i)
        {
            try
            {
                runFlow("selectStreamingException", TEST_MESSAGE);
                fail("Expected 'Table does not exist' exception.");
            }
            catch (MessagingException e)
            {
                assertThat("Iteration " + i, e.getMessage(), is("Table/View 'NOT_EXISTS' does not exist. (java.sql.SQLSyntaxErrorException)."));
            }
        }
    }

    @Test
    public void selectExceptionClosesPreviousResultSets() throws Exception
    {
        MuleMessage response = runFlow("selectExceptionClosesPreviousResultSets").getMessage();
        assertThat(response.getPayload(), is((Object) FALSE));
    }
}
