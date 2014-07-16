/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.db;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.DomainFunctionalTestCase;
import org.mule.tck.util.MuleDerbyTestDatabase;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DbSharedConnectorTestCase extends DomainFunctionalTestCase
{

    public static final String CLIENT_APP = "client";
    public static final String SERVER_APP = "server";

    private static MuleDerbyTestDatabase derbyTestDatabase = new MuleDerbyTestDatabase("database.name");

    private final String domainConfig;

    public DbSharedConnectorTestCase(String domainConfig)
    {
        this.domainConfig = domainConfig;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"domain/db/db-shared-connnector.xml"},
                {"domain/db/db-derby-shared-connnector.xml"}
        });
    }

    @BeforeClass
    public static void startDatabase() throws Exception
    {
        derbyTestDatabase.startDatabase();
    }

    @AfterClass
    public static void stopDatabase() throws SQLException
    {
        derbyTestDatabase.stopDatabase();
    }

    @Override
    protected String getDomainConfig()
    {
        return domainConfig;
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {
                new ApplicationConfig(CLIENT_APP, new String[] {"domain/db/db-client-app.xml"}),
                new ApplicationConfig(SERVER_APP, new String[] {"domain/db/db-server-app.xml"})
        };
    }

    @Test
    public void createJdbcRecordAndConsumeIt() throws Exception
    {
        final MuleContext clientAppMuleContext = getMuleContextForApp(CLIENT_APP);
        Flow flow = (Flow) clientAppMuleContext.getRegistry().lookupFlowConstruct("dbClientService");
        flow.process(AbstractMuleContextTestCase.getTestEvent(new Object(), clientAppMuleContext));
        MuleMessage response = getMuleContextForApp(SERVER_APP).getClient().request("vm://out", 5000);
        assertThat(response, notNullValue());
    }

}
