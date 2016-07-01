/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mule.module.db.integration.TestDbConfig.getOracleResource;
import static org.mule.module.db.integration.model.Contact.CONTACT2;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.OracleTestDatabase;

import java.sql.Struct;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateStructArrayUdtTestCase extends AbstractDbIntegrationTestCase
{

    public UpdateStructArrayUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        List<Object[]> params = new LinkedList<>();

        if (!getOracleResource().isEmpty())
        {
            params.add(new Object[] {"integration/config/oracle-unmapped-udt-db-config.xml", new OracleTestDatabase()});
        }

        return params;
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/update/update-udt-array-config.xml"};
    }

    @Test
    public void returnsStructArray() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://updatesStructArray", TEST_MESSAGE, null);

        assertThat(response.getPayload(), instanceOf(Object[].class));
        final Object[] arrayPayload = (Object[]) response.getPayload();
        assertThat(arrayPayload.length, equalTo(1));
        assertThat(arrayPayload[0], instanceOf(Struct.class));
        assertThat(((Struct) arrayPayload[0]).getAttributes(), equalTo(CONTACT2.getDetailsAsObjectArray()[0]));
    }
}
