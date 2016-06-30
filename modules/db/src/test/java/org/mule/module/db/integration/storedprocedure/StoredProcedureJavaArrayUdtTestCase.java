/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.storedprocedure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.module.db.integration.model.Contact.CONTACT1;
import static org.mule.module.db.integration.model.Region.SOUTHWEST;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.OracleTestDatabase;

import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureJavaArrayUdtTestCase extends AbstractDbIntegrationTestCase
{
    public StoredProcedureJavaArrayUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        List<Object[]> params = new LinkedList<>();

        if (!TestDbConfig.getOracleResource().isEmpty())
        {
            params.add(new Object[] {"integration/config/oracle-mapped-udt-db-config.xml", new OracleTestDatabase()});
        }

        return params;
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/storedprocedure/stored-procedure-udt-array-config.xml"};
    }

    @Before
    public void setupStoredProcedure() throws Exception
    {
        final DataSource dataSource = getDefaultDataSource();
        testDatabase.createStoredProcedureGetZipCodes(dataSource);
        testDatabase.createStoredProcedureGetContactDetails(dataSource);
    }

    @Test
    public void returnsDefaultArray() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://returnsDefaultArrayValue", TEST_MESSAGE, null);

        assertThat(response.getPayload(), Matchers.<Object>equalTo(SOUTHWEST.getZips()));
    }

    @Test
    public void returnsCustomArray() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://returnsCustomArrayValue", TEST_MESSAGE, null);

        assertThat(response.getPayload(), Matchers.<Object>equalTo(CONTACT1.getDetails()));
    }
}
