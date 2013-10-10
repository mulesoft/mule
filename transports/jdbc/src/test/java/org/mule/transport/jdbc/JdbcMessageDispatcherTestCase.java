/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.jdbc.test.TestDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JdbcMessageDispatcherTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testCustomResultSetHandlerIsNotIgnored() throws Exception
    {
        muleContext.start();
        JdbcConnector connector = new JdbcConnector(muleContext);
        
        connector.setQueryRunner(new TestQueryRunner());
        connector.setResultSetHandler(new TestResultSetHandler());
        connector.setDataSource(new TestDataSource());
        muleContext.getRegistry().registerConnector(connector);
        
        InboundEndpoint ep = muleContext.getEndpointFactory().getInboundEndpoint(
            "jdbc://select * from test");
        ep.request(0);
    }

    public static final class TestQueryRunner extends QueryRunner
    {
        @Override
        public Object query(Connection connection, String string, ResultSetHandler resultSetHandler,
                            Object[] objects) throws SQLException
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
