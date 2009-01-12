/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
        // TODO a bit weird api for test utils, refactor
        MuleDerbyTestUtils.cleanupDerbyDb(MuleDerbyTestUtils.setDerbyHome(), DATABASE_NAME);
        super.doTearDown();
    }
    
    @Override
    public Connector createConnector() throws Exception
    {
        JdbcConnector c = new JdbcConnector();
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
