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

import org.mule.RegistryContext;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.providers.jdbc.test.TestDataSource;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.util.object.SimpleObjectFactory;

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
        connector.setQueryRunner(new SimpleObjectFactory(TestQueryRunner.class));
        connector.setResultSetHandler(new SimpleObjectFactory(TestResultSetHandler.class));
        connector.setDataSourceFactory(new SimpleObjectFactory(TestDataSource.class));
        RegistryContext.getRegistry().registerConnector(connector);
        UMOImmutableEndpoint ep = new ImmutableMuleEndpoint("jdbc://select * from test", false);
        ep.receive(0);
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
