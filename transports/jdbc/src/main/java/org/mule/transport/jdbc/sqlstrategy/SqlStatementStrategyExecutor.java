/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
