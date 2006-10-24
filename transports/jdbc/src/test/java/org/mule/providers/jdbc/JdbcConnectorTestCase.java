/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import java.util.HashMap;
import java.util.Map;

import org.hsqldb.jdbc.jdbcDataSource;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

public class JdbcConnectorTestCase extends AbstractConnectorTestCase
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#getConnector()
     */
    public UMOConnector getConnector() throws Exception
    {
        JdbcConnector c = new JdbcConnector();
        c.setName("JdbcConnector");
        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("hsqldb:.");
        ds.setUser("sa");
        c.setDataSource(ds);
        c.setPollingFrequency(1000);
        c.initialise();
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
