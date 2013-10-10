/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc;

import java.sql.Connection;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.jdbc.sqlstrategy.SqlStatementStrategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transport.jdbc.sqlstrategy.SqlStatementStrategyExecutor;

/**
 * The Jdbc Message dispatcher is responsible for executing SQL queries against a
 * database.
 */
public class JdbcMessageDispatcher extends AbstractMessageDispatcher
{

    protected static Log staticLogger = LogFactory.getLog(AbstractMessageDispatcher.class);

    protected JdbcConnector connector;
    private SqlStatementStrategyExecutor sqlStatementExecutor = new SqlStatementStrategyExecutor();

    public JdbcMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JdbcConnector) endpoint.getConnector();
    }

    @Override
    protected void doDispose()
    {
        // template method
    } 
    
    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Dispatch event: " + event);
        }
        
        doSend(event);
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        // Use a strategy pattern to choose a particular strategy to handle the SQL request
        JdbcConnector jdbcConnector = (JdbcConnector) endpoint.getConnector();
        String statement = jdbcConnector.getStatement(endpoint);
        Object payload = event.getMessage().getPayload();
        
        SqlStatementStrategy strategy = 
            jdbcConnector.getSqlStatementStrategyFactory().create(statement, payload);
        Connection connection = (Connection) connector.getTransactionalResource(endpoint);
        return sqlStatementExecutor.execute(strategy,jdbcConnector, endpoint, event, event.getTimeout(), connection);
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }
}
