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

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.transport.jdbc.test.TestDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

public class JdbcMessageDispatcherTestCase extends AbstractMuleTestCase
{
    public void testCustomResultSetHandlerIsNotIgnored() throws Exception
    {
        JdbcConnector connector = new JdbcConnector();
        
        connector.setQueryRunner(new TestQueryRunner());
        connector.setResultSetHandler(new TestResultSetHandler());
        connector.setDataSource(new TestDataSource());
        
        connector.setMuleContext(muleContext);
        //muleContext.applyLifecycle(connector);
        muleContext.getRegistry().registerConnector(connector);
        
        
        ImmutableEndpoint ep = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            "jdbc://select * from test");
        ep.request(0);
    }

    public static final class TestQueryRunner extends QueryRunner
    {

        public Object query(Connection connection,
                            String string,
                            Object[] objects,
                            ResultSetHandler resultSetHandler) throws SQLException
        {
            assertTrue("Custom result set handler has been ignored.",
                resultSetHandler instanceof TestResultSetHandler);
            return new Object();
        }
    }

    public static final class TestResultSetHandler implements ResultSetHandler
    {

        public Object handle(ResultSet resultSet) throws SQLException
        {
            return new Object();
        }
    }
}
