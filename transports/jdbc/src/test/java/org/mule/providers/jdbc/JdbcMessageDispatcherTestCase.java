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

import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import com.mockobjects.dynamic.Mock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

public class JdbcMessageDispatcherTestCase extends AbstractMuleTestCase
{

    public void testCustomResultSetHandlerIsNotIgnored() throws Exception
    {
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint("jdbc://select * from test", false);
        JdbcConnector connector = (JdbcConnector)ep.getConnector();
        connector.setQueryRunner(TestQueryRunner.class.getName());
        connector.setResultSetHandler(TestResultSetHandler.class.getName());
        connector.setDataSource(getDataSource());
        connector.initialise();
        ep.receive(0);
    }

    protected DataSource getDataSource()
    {
        Mock mockDataSource = new Mock(DataSource.class);
        Mock mockConnection = new Mock(Connection.class);

        mockDataSource.expectAndReturn("getConnection", mockConnection.proxy());

        mockConnection.expectAndReturn("getAutoCommit", false);
        mockConnection.expect("commit");
        mockConnection.expect("close");

        return (DataSource)mockDataSource.proxy();
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
