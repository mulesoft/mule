/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
