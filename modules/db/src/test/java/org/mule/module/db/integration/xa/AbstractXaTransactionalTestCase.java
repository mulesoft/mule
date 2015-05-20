/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.xa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.model.Planet.MARS;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.DerbyTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;
import org.mule.transport.NullPayload;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public abstract class AbstractXaTransactionalTestCase extends AbstractDbIntegrationTestCase
{

    public AbstractXaTransactionalTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return Collections.singletonList(new Object[] {"integration/config/derby-xa-db-config.xml", new DerbyTestDatabase()});
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {getTransactionManagerResource(), "integration/xa/xa-transactional-config.xml"};
    }

    protected abstract String getTransactionManagerResource();

    @Test
    public void commitsChanges() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testCommit", TEST_MESSAGE, null);

        assertEquals(1, response.getPayload());

        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }

    @Test
    public void rollbacksChanges() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testRollback", TEST_MESSAGE, null);

        assertTrue(response.getPayload() instanceof NullPayload);

        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", MARS.getName()), new Field("POSITION", 4)));
    }

    @Test
    public void commitsChangesWhenMpIsNotTransactionalOnRollback() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://rollbackWithNonTransactionalMP", TEST_MESSAGE, null);

        assertTrue(response.getPayload() instanceof NullPayload);

        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }

    @Test
    public void commitsChangesWhenMpIsNotTransactionalOnCommit() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://commitWithNonTransactionalMP", TEST_MESSAGE, null);

        assertTrue(response.getPayload() instanceof NullPayload);

        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }

}
