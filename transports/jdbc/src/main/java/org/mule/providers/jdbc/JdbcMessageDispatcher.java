/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageAdapter;
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

    public JdbcMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JdbcConnector) endpoint.getConnector();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractMessageDispatcher#doDispose()
     */
    protected void doDispose()
    {
        // template method
    }
    
    protected void executeWriteStatement(UMOEvent event, String writeStmt) throws Exception
    {
        List paramNames = new ArrayList();
        writeStmt = connector.parseStatement(writeStmt, paramNames);

        Object[] paramValues = connector.getParams(endpoint, paramNames, new MuleMessage(
            event.getTransformedMessage()), this.endpoint.getEndpointURI().getAddress());

        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        Connection con = null;
        try
        {
            con = this.connector.getConnection();
            
            if ("call".equalsIgnoreCase(writeStmt.substring(0, 4)))
            {
                writeStmt = STORED_PROCEDURE_PREFIX + writeStmt + STORED_PROCEDURE_SUFFIX;
            }
            
            int nbRows = connector.createQueryRunner().update(con, writeStmt, paramValues);
            if (nbRows != 1)
            {
                logger.warn("Row count for write should be 1 and not " + nbRows);
            }
            if (tx == null)
            {
                JdbcUtils.commitAndClose(con);
            }
            logger.debug("Event dispatched succesfuly");
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
    
    protected String getStatement(UMOImmutableEndpoint endpoint)
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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractMessageDispatcher#doDispatch(org.mule.umo.UMOEvent)
     */
    protected void doDispatch(UMOEvent event) throws Exception
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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractMessageDispatcher#doSend(org.mule.umo.UMOEvent)
     */
    protected UMOMessage doSend(UMOEvent event) throws Exception
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
    protected static UMOMessage executeRequest(long timeout, UMOEvent event,
                                               JdbcConnector connector, UMOImmutableEndpoint endpoint) throws Exception
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
                result = connector.createQueryRunner().query(con, readStmt,
                                                             connector.getParams(endpoint,
                                                                                 readParams,
                                                                                 event!=null ? event.getMessage() : null,
                                                                                 endpoint.getEndpointURI().getAddress()),
                                                             connector.createResultSetHandler());
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
                    return null;
                }
            }
            while (true);
            if (ackStmt != null)
            {
                int nbRows = connector.createQueryRunner().update(con, ackStmt,
                                                                  connector.getParams(endpoint, ackParams, result, ackStmt));
                if (nbRows != 1)
                {
                    staticLogger.warn("Row count for ack should be 1 and not " + nbRows);
                }
            }
            UMOMessageAdapter msgAdapter = connector.getMessageAdapter(result);
            UMOMessage message = new MuleMessage(msgAdapter);
            JdbcUtils.commitAndClose(con);
            return message;
        }
        catch (Exception e)
        {
            JdbcUtils.rollbackAndClose(con);
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
