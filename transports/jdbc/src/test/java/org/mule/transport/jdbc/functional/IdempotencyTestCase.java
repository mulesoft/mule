/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.DateUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import org.apache.commons.dbutils.QueryRunner;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class IdempotencyTestCase extends AbstractJdbcFunctionalTestCase
{

    public IdempotencyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{            
            {ConfigVariant.FLOW, AbstractJdbcFunctionalTestCase.getConfig() + ",jdbc-store.xml"}
        });
    }      
    
    @Test
    public void testIdempotencySequential() throws Exception
    {
        MuleClient client = muleContext.getClient();

        int id = Integer.valueOf(DateUtils.formatTimeStamp(Calendar.getInstance().getTime(), "hhmmss"));
        String valueExpression = DateUtils.formatTimeStamp(Calendar.getInstance().getTime(), "ssmmhh");

        MuleMessage message = new DefaultMuleMessage("hi", muleContext);
        message.setProperty("originalId", id, PropertyScope.OUTBOUND);
        message.setProperty("valueId", valueExpression, PropertyScope.OUTBOUND);
        client.dispatch("vm://in", message);

        MuleMessage requestMessage = client.request("vm://forProcessing", FunctionalTestCase.RECEIVE_TIMEOUT);
        assertNotNull(requestMessage);

        message = new DefaultMuleMessage("hi", muleContext);
        message.setProperty("originalId", id, PropertyScope.OUTBOUND);
        message.setProperty("valueId", valueExpression, PropertyScope.OUTBOUND);
        client.send("vm://in", message);

        requestMessage = client.request("vm://duplicated", FunctionalTestCase.RECEIVE_TIMEOUT);
        assertNotNull(requestMessage);
    }

    @Override
    protected void createTable() throws Exception
    {
        super.createTable();
        QueryRunner qr = jdbcConnector.getQueryRunner();
        qr.update(jdbcConnector.getConnection(), "CREATE TABLE IDS(K VARCHAR(255) NOT NULL PRIMARY KEY, VALUE VARCHAR(255))");
    }

    @Override
    protected void deleteTable() throws Exception
    {
        super.deleteTable();
        QueryRunner qr = jdbcConnector.getQueryRunner();
        qr.update(jdbcConnector.getConnection(), "DELETE FROM IDS");
    }
}
