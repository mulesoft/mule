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

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.jdbc.sqlstrategy.SqlStatementStrategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Jdbc Message dispatcher is responsible for executing SQL queries against a
 * database.
 */
public class JdbcMessageDispatcher extends AbstractMessageDispatcher
{

    protected static Log staticLogger = LogFactory.getLog(AbstractMessageDispatcher.class);

    protected JdbcConnector connector;

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

    protected void doDispatch(MuleEvent event) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Dispatch event: " + event);
        }
        
        doSend(event);
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        // Use a strategy pattern to choose a particular strategy to handle the SQL request
        ImmutableEndpoint endpoint = event.getEndpoint();
        JdbcConnector connector = (JdbcConnector) endpoint.getConnector();
        String statement = connector.getStatement(endpoint);
        Object payload = event.transformMessage();
        
        SqlStatementStrategy strategy = 
            connector.getSqlStatementStrategyFactory().create(statement, payload);
        return strategy.executeStatement(connector, endpoint, event, event.getTimeout());
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
