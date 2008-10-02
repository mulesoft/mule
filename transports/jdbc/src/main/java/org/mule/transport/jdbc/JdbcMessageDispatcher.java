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
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.jdbc.sqlstrategy.SQLStrategy;

import java.sql.Connection;

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
    
    private Connection jdbcConnection;
    
    /** Are we inside a transaction? */
    private boolean transaction;

    public JdbcMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JdbcConnector) endpoint.getConnector();
        useStrictConnectDisconnect = true;
    }

    protected void doDispose()
    {
        // template method
    }
    
    //@Override
    protected void doPreConnect(MuleEvent event) throws Exception
    {
        transaction = (TransactionCoordination.getInstance().getTransaction() != null);
    }

    protected void doConnect() throws Exception
    {
        if (jdbcConnection == null)
        {
            jdbcConnection = connector.getConnection();
        }
    }

    protected void doDisconnect() throws Exception
    {
        if (!transaction)
        {
            jdbcConnection.close();
            jdbcConnection = null;
        }
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
        String statement = connector.getStatement(endpoint);

        //Use a strategy pattern to choose a particular strategy to handle the SQL request
        SQLStrategy strategy = connector.getSqlStrategyFactory().create(statement, event.getMessage().getPayload());
        return strategy.executeStatement(jdbcConnection, endpoint, event, event.getTimeout());      
    }
}
