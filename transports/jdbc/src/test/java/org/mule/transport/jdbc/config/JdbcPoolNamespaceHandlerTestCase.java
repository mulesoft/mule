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

import org.enhydra.jdbc.standard.StandardDataSource;

public class JdbcPoolNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "jdbc-pool-namespace-config.xml";
    }

    public void testSingleton()
    {
        StandardDataSource pool1 = lookupPool("default-oracle");
        StandardDataSource pool2 = lookupPool("default-oracle");
        assertSame(pool1, pool2);
    }

    public void testDefault()
    {
        StandardDataSource pool = lookupPool("default-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1521:orcl", pool.getUrl());
        assertEquals("oracle.jdbc.driver.OracleDriver", pool.getDriverName());
        assertEquals("scott", pool.getUser());
        assertEquals("tiger", pool.getPassword());
    }

    public void testCustomUrl()
    {
        StandardDataSource pool = lookupPool("custom-url-oracle");
        assertEquals("jdbc:oracle:thin:@some-other-host:1522:mule", pool.getUrl());
    }

    public void testCustomHost()
    {
        StandardDataSource pool = lookupPool("custom-host-oracle");
        assertEquals("jdbc:oracle:thin:@some-other-host:1521:orcl", pool.getUrl());
    }

    public void testCustomPort()
    {
        StandardDataSource pool = lookupPool("custom-port-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1522:orcl", pool.getUrl());
    }

    public void testCustomInstance()
    {
        StandardDataSource pool = lookupPool("custom-instance-oracle");
        assertEquals("jdbc:oracle:thin:@localhost:1521:mule", pool.getUrl());
    }

    private StandardDataSource lookupPool(String key)
    {
        Object object = muleContext.getRegistry().lookupObject(key);
        assertNotNull(object);
        assertTrue(object instanceof StandardDataSource);

        return (StandardDataSource) object;
    }
}
