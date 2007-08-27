/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

import java.util.HashMap;
import java.util.Map;

import org.hsqldb.jdbc.jdbcDataSource;

public class JdbcConnectorTestCase extends AbstractConnectorTestCase
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getConnector()
     */
    // @Override
    public UMOConnector createConnector() throws Exception
    {
        JdbcConnector c = new JdbcConnector();
        c.setName("JdbcConnector");
        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("hsqldb:.");
        ds.setUser("sa");
        c.setDataSource(ds);
        c.setPollingFrequency(1000);
        return c;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getValidMessage()
     */
    public Object getValidMessage() throws Exception
    {
        Map map = new HashMap();
        return map;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getTestEndpointURI()
     */
    public String getTestEndpointURI()
    {
        return "jdbc://test?sql=SELECT * FROM TABLE";
    }

}
