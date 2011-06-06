/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.config;

import org.mule.tck.FunctionalTestCase;

import java.sql.Connection;

import org.enhydra.jdbc.standard.StandardDataSource;

public class JdbcDataSourceNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "jdbc-data-source-namespace-config.xml";
    }

    public void testSingleton()
    {
        StandardDataSource ds1 = lookupDataSource("default-oracle");
        StandardDataSource ds2 = lookupDataSource("default-oracle");
        assertSame(ds1, ds2);
    }

    public void testDefault()
    {
        StandardDataSource source = lookupDataSource("default-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", source.getUrl());
        assertEquals("oracle.jdbc.driver.OracleDriver", source.getDriverName());
        assertEquals("scott", source.getUser());
        assertEquals("tiger", source.getPassword());
    }

    public void testCustomUrl()
    {
        StandardDataSource source = lookupDataSource("custom-url-oracle");
        assertEquals("jdbc:oracle:thin:@some-other-host:1522:mule", source.getUrl());
    }

    public void testCustomHost()
    {
        StandardDataSource source = lookupDataSource("custom-host-oracle");
        assertEquals("jdbc:oracle:thin:@some-other-host:1521:orcl", source.getUrl());
    }

    public void testCustomPort()
    {
        StandardDataSource source = lookupDataSource("custom-port-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1522:orcl", source.getUrl());
    }

    public void testCustomInstance()
    {
        StandardDataSource source = lookupDataSource("custom-instance-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1521:mule", source.getUrl());
    }

    public void testCustomDataSourceProperties()
    {
        StandardDataSource source = lookupDataSource("custom-ds-properties");
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, source.getTransactionIsolation());
    }

    private StandardDataSource lookupDataSource(String key)
    {
        Object object = muleContext.getRegistry().lookupObject(key);
        assertNotNull(object);
        assertTrue(object instanceof StandardDataSource);

        return (StandardDataSource) object;
    }
}
