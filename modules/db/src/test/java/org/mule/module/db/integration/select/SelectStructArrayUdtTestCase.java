/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.mule.module.db.integration.TestDbConfig.getOracleResource;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.module.db.integration.model.Contact.CONTACT1;
import static org.mule.module.db.integration.model.Contact.CONTACT2;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.OracleTestDatabase;
import org.mule.module.db.integration.model.Record;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectStructArrayUdtTestCase extends AbstractDbIntegrationTestCase
{
    public SelectStructArrayUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
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
        return new String[] {"integration/select/select-udt-array-config.xml"};
    }

    @Test
    public void returnsCustomArray() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://returnsCustomArray", TEST_MESSAGE, null);

        assertRecords(response.getPayload(), new Record(new Field("CONTACT_NAME", CONTACT1.getName()), new Field("DETAILS", CONTACT1.getDetailsAsObjectArray()[0])),
                      new Record(new Field("CONTACT_NAME", CONTACT2.getName()), new Field("DETAILS", CONTACT2.getDetailsAsObjectArray()[0])));
    }
}
