/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.FunctionalTestCase;

import java.sql.Connection;

import org.enhydra.jdbc.standard.StandardDataSource;
import org.junit.Test;

public class JdbcDataSourceNamespaceHandlerTestCase extends FunctionalTestCase
{
    public JdbcDataSourceNamespaceHandlerTestCase()
    {
        super();
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "jdbc-data-source-namespace-config.xml";
    }

    @Test
    public void testSingleton()
    {
        StandardDataSource ds1 = lookupDataSource("default-oracle");
        StandardDataSource ds2 = lookupDataSource("default-oracle");
        assertSame(ds1, ds2);
    }

    @Test
    public void testCustomDataSourceProperties()
    {
        StandardDataSource source = lookupDataSource("custom-ds-properties");
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, source.getTransactionIsolation());
        assertEquals(42, source.getLoginTimeout());
    }

    @Test
    public void testOracleDefaults()
    {
        StandardDataSource source = lookupDataSource("default-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", source.getUrl());
        assertEquals("oracle.jdbc.driver.OracleDriver", source.getDriverName());
        assertEquals(-1, source.getTransactionIsolation());
        assertEquals("scott", source.getUser());
        assertEquals("tiger", source.getPassword());
    }

    @Test
    public void testOracleCustomUrl()
    {
        StandardDataSource source = lookupDataSource("custom-url-oracle");
        assertEquals("jdbc:oracle:thin:@some-other-host:1522:mule", source.getUrl());
    }

    @Test
    public void testOracleCustomHost()
    {
        StandardDataSource source = lookupDataSource("custom-host-oracle");
        assertEquals("jdbc:oracle:thin:@some-other-host:1521:orcl", source.getUrl());
    }

    @Test
    public void testOracleCustomPort()
    {
        StandardDataSource source = lookupDataSource("custom-port-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1522:orcl", source.getUrl());
    }

    @Test
    public void testOracleCustomInstance()
    {
        StandardDataSource source = lookupDataSource("custom-instance-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1521:mule", source.getUrl());
    }

    @Test
    public void testMysqlDefaults()
    {
        StandardDataSource source = lookupDataSource("default-mysql");
        assertEquals("jdbc:mysql://localhost/mule", source.getUrl());
        assertEquals("com.mysql.jdbc.Driver", source.getDriverName());
        assertEquals("mysql", source.getUser());
        assertEquals("secret", source.getPassword());
    }

    @Test
    public void testMysqlCustomUrl()
    {
        StandardDataSource source = lookupDataSource("custom-url-mysql");
        assertEquals("jdbc:mysql://mule-db-host:3306/mule", source.getUrl());
    }

    @Test
    public void testMysqlCustomHost()
    {
        StandardDataSource source = lookupDataSource("custom-host-mysql");
        assertEquals("jdbc:mysql://some-other-host/mule", source.getUrl());
    }

    @Test
    public void testMysqlCustomPort()
    {
        StandardDataSource source = lookupDataSource("custom-port-mysql");
        assertEquals("jdbc:mysql://localhost:4242/mule", source.getUrl());
    }

    @Test
    public void testPostgresqlDefaults()
    {
        StandardDataSource source = lookupDataSource("default-postgresql");
        assertEquals("jdbc:postgresql://localhost/mule", source.getUrl());
        assertEquals("org.postgresql.Driver", source.getDriverName());
        assertEquals("postgres", source.getUser());
        assertEquals("secret", source.getPassword());
    }

    @Test
    public void testPostgresqlCustomUrl()
    {
        StandardDataSource source = lookupDataSource("custom-url-postgresql");
        assertEquals("jdbc:postgresql://mule-db-host:5432/mule", source.getUrl());
    }

    @Test
    public void testPostgresqlCustomHost()
    {
        StandardDataSource source = lookupDataSource("custom-host-postgresql");
        assertEquals("jdbc:postgresql://some-other-host/mule", source.getUrl());
    }

    @Test
    public void testPostgresqlCustomPort()
    {
        StandardDataSource source = lookupDataSource("custom-port-postgresql");
        assertEquals("jdbc:postgresql://localhost:5433/mule", source.getUrl());
    }

    @Test
    public void testDerbyDefaults()
    {
        StandardDataSource source = lookupDataSource("default-derby");
        assertEquals("jdbc:derby:memory:mule", source.getUrl());
        assertEquals("org.apache.derby.jdbc.EmbeddedDriver", source.getDriverName());
    }

    @Test
    public void testDerbyCustomUrl()
    {
        StandardDataSource source = lookupDataSource("custom-url-derby");
        assertEquals("jdbc:derby:muleEmbedded", source.getUrl());
    }

    @Test
    public void testDerbyCreateDatabase()
    {
        StandardDataSource source = lookupDataSource("create-database-derby");
        assertEquals("jdbc:derby:memory:mule;create=true", source.getUrl());
    }

    private StandardDataSource lookupDataSource(String key)
    {
        Object object = muleContext.getRegistry().lookupObject(key);
        assertNotNull(object);
        assertTrue(object instanceof StandardDataSource);

        return (StandardDataSource) object;
    }
}
