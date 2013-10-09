/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc;

import org.mule.api.transport.Connector;
import org.mule.tck.util.MuleDerbyTestUtils;
import org.mule.transport.AbstractConnectorTestCase;

import java.util.HashMap;
import java.util.Map;

import org.apache.derby.jdbc.EmbeddedDataSource;

public class JdbcConnectorTestCase extends AbstractConnectorTestCase
{

    private static final String DATABASE_NAME = "embeddedDb";

    @Override
    protected void doSetUp() throws Exception
    {
        MuleDerbyTestUtils.createDataBase(DATABASE_NAME);
        super.doSetUp();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        MuleDerbyTestUtils.cleanupDerbyDb(DATABASE_NAME);
        super.doTearDown();
    }
    
    @Override
    public Connector createConnector() throws Exception
    {
        JdbcConnector c = new JdbcConnector(muleContext);
        EmbeddedDataSource embeddedDS = new EmbeddedDataSource();
        embeddedDS.setDatabaseName(DATABASE_NAME);
        c.setName("JdbcConnector");
        c.setDataSource(embeddedDS);
        c.setPollingFrequency(1000);
        return c;
    }

    public Object getValidMessage() throws Exception
    {
        Map map = new HashMap();
        return map;
    }

    public String getTestEndpointURI()
    {
        return "jdbc://test?sql=SELECT * FROM TABLE";
    }
}
