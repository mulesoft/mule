/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.DerbyTestDatabase;
import org.mule.module.db.internal.domain.database.DbConfig;

import java.util.Collections;
import java.util.List;

import org.enhydra.jdbc.standard.StandardDataSource;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class GenericDbConfigTestCase extends AbstractDbIntegrationTestCase
{

    private static final String EXPECTED_DRIVER_CLASSNAME = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String EXPECTED_URL = "jdbc:derby:target/muleEmbeddedDB;sql.enforce_strict_size=true;create=true";
    private static final String EXPECTED_PASSWORD = "passwordTest";
    private static final String EXPECTED_USER = "userTest";

    public GenericDbConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return Collections.singletonList(new Object[] {"integration/config/generic-db-config.xml", new DerbyTestDatabase()});
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[0];
    }

    @Test
    public void configPropertiesAreTheExpected() throws Exception
    {
        final MuleEvent muleEvent = getTestEvent(TEST_MESSAGE);

        DbConfig dbConfig = ((org.mule.module.db.internal.resolver.database.DbConfigResolver) muleContext.getRegistry().lookupObject("dbConfig")).resolve(muleEvent);
        StandardDataSource dataSource = (StandardDataSource) dbConfig.getDataSource();

        assertThat(dataSource.getUser(), equalTo(EXPECTED_USER));
        assertThat(dataSource.getPassword(), equalTo(EXPECTED_PASSWORD));
        assertThat(dataSource.getUrl(), equalTo(EXPECTED_URL));
        assertThat(dataSource.getDriverName(), equalTo(EXPECTED_DRIVER_CLASSNAME));
    }
}