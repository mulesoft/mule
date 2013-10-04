/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.sqlstrategy;

import java.sql.Connection;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.transport.jdbc.JdbcUtils;

public class SqlStatementStrategyExecutor
{
    public MuleMessage execute(SqlStatementStrategy strategy,JdbcConnector connector, ImmutableEndpoint endpoint,
                                            MuleEvent event, long timeout, Connection connection) throws Exception
    {
        try
        {
            MuleMessage muleMessage = strategy.executeStatement(connector, endpoint, event, timeout, connection);
            if (TransactionCoordination.getInstance().getTransaction() == null)
            {
                JdbcUtils.commitAndClose(connection);
            }
            return muleMessage;
        }
        catch (Exception e)
        {
            if (TransactionCoordination.getInstance().getTransaction() == null)
            {
                JdbcUtils.rollbackAndClose(connection);
            }
            throw e;
        }
    }
}
