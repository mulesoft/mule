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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.api.transport.MessageAdapter;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.util.ArrayUtils;
import org.mule.util.StringUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Jdbc Message dispatcher is responsible for executing SQL queries against a
 * database.
 */
public class JdbcMessageDispatcher extends AbstractMessageDispatcher
{

    private static Log staticLogger = LogFactory.getLog(AbstractMessageDispatcher.class);

    private JdbcConnector connector;
    private static final String STORED_PROCEDURE_PREFIX = "{ ";
    private static final String STORED_PROCEDURE_SUFFIX = " }";

    public JdbcMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JdbcConnector) endpoint.getConnector();
    }

    protected void doDispose()
    {
        // template method
    }
    
    protected void executeWriteStatement(MuleEvent event, String writeStmt) throws Exception
    {
        List paramNames = new ArrayList();
        writeStmt = connector.parseStatement(writeStmt, paramNames);

        Object[] paramValues = connector.getParams(endpoint, paramNames, new DefaultMuleMessage(
            event.transformMessage()), this.endpoint.getEndpointURI().getAddress());

        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        Connection con = null;
        try
        {
            con = this.connector.getConnection();
            
            if ("call".equalsIgnoreCase(writeStmt.substring(0, 4)))
            {
                writeStmt = STORED_PROCEDURE_PREFIX + writeStmt + STORED_PROCEDURE_SUFFIX;
            }
            
            if (logger.isDebugEnabled())
            {
                logger.debug("SQL UPDATE: " + writeStmt + ", params = " + ArrayUtils.toString(paramValues));
            }
            int nbRows = connector.getQueryRunner().update(con, writeStmt, paramValues);
            if (nbRows != 1)
            {
                logger.warn("Row count for write should be 1 and not " + nbRows);
            }
            if (tx == null)
            {
                JdbcUtils.commitAndClose(con);
            }
            logger.debug("MuleEvent dispatched succesfuly");
        }
        catch (Exception e)
        {
            logger.debug("Error dispatching event: " + e.getMessage(), e);
            if (tx == null)
            {
                JdbcUtils.rollbackAndClose(con);
            }
            throw e;
        }
    }
    
    protected String getStatement(ImmutableEndpoint endpoint)
    {
        String writeStmt = endpoint.getEndpointURI().getAddress();
        String str;
        if ((str = this.connector.getQuery(endpoint, writeStmt)) != null)
        { 
            writeStmt = str;
        }
        writeStmt = StringUtils.trimToEmpty(writeStmt);
        if (StringUtils.isBlank(writeStmt))
        {
            throw new IllegalArgumentException("Missing statement");
        }
        
        return writeStmt;
    }
    
    protected boolean isWriteStatement(String writeStmt)
    {
        if (!"insert".equalsIgnoreCase(writeStmt.substring(0, 6))
                        && !"update".equalsIgnoreCase(writeStmt.substring(0, 6))
                        && !"delete".equalsIgnoreCase(writeStmt.substring(0, 6))
                        && !"merge".equalsIgnoreCase(writeStmt.substring(0, 5))
                        && !"call".equalsIgnoreCase(writeStmt.substring(0, 4)))
        {
            return false;
        }
        
        return true;
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Dispatch event: " + event);
        }
        
        String writeStmt = getStatement(event.getEndpoint());
        
        if (!isWriteStatement(writeStmt))
        {
            throw new IllegalArgumentException(
                "Write statement should be an insert / update / delete / merge sql statement, or a stored-procedure call");
        }
        
        this.executeWriteStatement(event, writeStmt);
        
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        String statement = getStatement(event.getEndpoint());
        
        if (isWriteStatement(statement))
        {
            executeWriteStatement(event, statement);
            return event.getMessage();
        }
        
        return executeRequest(event.getTimeout(),event, connector, endpoint);
        
    }

    /**
     * This does work for both dispatcher and requester
     *
     * @param timeout
     * @param event
     * @param connector
     * @param endpoint
     * @return
     * @throws Exception
     */
    protected static MuleMessage executeRequest(long timeout, MuleEvent event,
                                               JdbcConnector connector, ImmutableEndpoint endpoint) throws Exception
    {
        if (staticLogger.isDebugEnabled())
        {
            staticLogger.debug("Trying to receive a message with a timeout of " + timeout);
        }

        String[] stmts = connector.getReadAndAckStatements(endpoint);
        String readStmt = stmts[0];
        String ackStmt = stmts[1];
        List readParams = new ArrayList();
        List ackParams = new ArrayList();
        readStmt = connector.parseStatement(readStmt, readParams);
        ackStmt = connector.parseStatement(ackStmt, ackParams);

        Connection con = null;
        long t0 = System.currentTimeMillis();
        Transaction tx  = TransactionCoordination.getInstance().getTransaction();
        try
        {
            con = connector.getConnection();
            if (timeout < 0)
            {
                timeout = Long.MAX_VALUE;
            }
            Object result;
            do
            {
                Object[] params = connector.getParams(endpoint, readParams,
                    event!=null ? event.getMessage() : null,
                    endpoint.getEndpointURI().getAddress());
                if (staticLogger.isDebugEnabled())
                {
                    staticLogger.debug("SQL QUERY: " + readStmt + ", params = " + ArrayUtils.toString(params));
                }
                result = connector.getQueryRunner().query(con, readStmt, params, connector.getResultSetHandler());
                if (result != null)
                {
                    if (staticLogger.isDebugEnabled())
                    {
                        staticLogger.debug("Received: " + result);
                    }
                    break;
                }
                long sleep = Math.min(connector.getPollingFrequency(),
                                      timeout - (System.currentTimeMillis() - t0));
                if (sleep > 0)
                {
                    if (staticLogger.isDebugEnabled())
                    {
                        staticLogger.debug("No results, sleeping for " + sleep);
                    }
                    Thread.sleep(sleep);
                }
                else
                {
                    staticLogger.debug("Timeout");
                    JdbcUtils.rollbackAndClose(con);
                    return null;
                }
            }
            while (true);
            if (ackStmt != null)
            {
                Object[] params = connector.getParams(endpoint, ackParams, result, ackStmt);
                if (staticLogger.isDebugEnabled())
                {
                    staticLogger.debug("SQL UPDATE: " + ackStmt + ", params = " + ArrayUtils.toString(params));
                }
                int nbRows = connector.getQueryRunner().update(con, ackStmt, params);
                if (nbRows != 1)
                {
                    staticLogger.warn("Row count for ack should be 1 and not " + nbRows);
                }
            }
            MessageAdapter msgAdapter = connector.getMessageAdapter(result);
            MuleMessage message = new DefaultMuleMessage(msgAdapter);
            if (tx == null)
            {
                JdbcUtils.commitAndClose(con);
            }
            
            return message;
        }
        catch (Exception e)
        {
            if (tx == null)
            {
                JdbcUtils.rollbackAndClose(con);
            }
            throw e;
        }

    }


    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

}
