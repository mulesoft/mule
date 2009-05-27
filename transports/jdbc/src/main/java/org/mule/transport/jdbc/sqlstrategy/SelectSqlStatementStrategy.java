/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.sqlstrategy;

/**
 * Implements strategy for handling normal select statements + acks.  
 * 
 */

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transaction.Transaction;
import org.mule.api.transport.MessageAdapter;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.transport.jdbc.JdbcUtils;
import org.mule.util.ArrayUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public  class SelectSqlStatementStrategy
    implements SqlStatementStrategy
{
    protected transient Logger logger = Logger.getLogger(getClass());
    
    public MuleMessage executeStatement(JdbcConnector connector,
            ImmutableEndpoint endpoint,MuleEvent event,long timeout) throws Exception
    {
            
        logger.debug("Trying to receive a message with a timeout of " + timeout);
        
        String[] stmts = connector.getReadAndAckStatements(endpoint);
        
        //Unparsed SQL statements (with #[foo] parameters)
        String readStmt = stmts[0];
        String ackStmt = stmts[1];
        
        //Storage for params (format is #[foo])
        List readParams = new ArrayList();
        List ackParams = new ArrayList();
        
        //Prepared statement form (with ? placeholders instead of #[foo] params)
        readStmt = connector.parseStatement(readStmt, readParams);
        ackStmt = connector.parseStatement(ackStmt, ackParams);

        Connection con = null;
        long t0 = System.currentTimeMillis();
        Transaction tx  = TransactionCoordination.getInstance().getTransaction();
        try
        {
            con = connector.getConnection();
            
            //This method is used in both JDBCMessageDispatcher and JDBCMessageRequester.  
            //JDBCMessageRequester specifies a finite timeout.
            if (timeout < 0)
            {
                timeout = Long.MAX_VALUE;
            }
            Object result;
            
            //do-while loop.  execute query until there's a result or timeout exceeded
            do
            {
                //Get the actual param values from the message.
                Object[] params = connector.getParams(endpoint, readParams,
                    event != null ? event.getMessage() : null,
                    endpoint.getEndpointURI().getAddress());
                
                if (logger.isDebugEnabled())
                {
                    logger.debug("SQL QUERY: " + readStmt + ", params = " + ArrayUtils.toString(params));
                }

                //Perform actual query
                result = connector.getQueryRunner().query(con, readStmt, params, connector.getResultSetHandler());
                
                if (result != null)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("SQL query received a result: " + result);
                    }
                    else if (logger.isInfoEnabled())
                    {
                        logger.info("SQL query received a result");
                    }
                    break;
                }
                long sleep = Math.min(connector.getPollingFrequency(),
                                      timeout - (System.currentTimeMillis() - t0));
                if (sleep > 0)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("No results, sleeping for " + sleep);
                    }
                    Thread.sleep(sleep);
                }
                else
                {
                    logger.debug("Timeout");
                    JdbcUtils.rollbackAndClose(con);
                    return null;
                }
            } while (true);
            
            //Execute ack statement
            if (ackStmt != null)
            {
                Object[] params = connector.getParams(endpoint, ackParams, new DefaultMuleMessage(result, (Map)null), ackStmt);
                if (logger.isDebugEnabled())
                {
                    logger.debug("SQL UPDATE: " + ackStmt + ", params = " + ArrayUtils.toString(params));
                }
                int nbRows = connector.getQueryRunner().update(con, ackStmt, params);
                if (nbRows != 1)
                {
                    logger.warn("Row count for ack should be 1 and not " + nbRows);
                }
            }
            
            // Package up result
            MessageAdapter msgAdapter = null;
            if (event != null)
            {
                msgAdapter = connector.getMessageAdapter(result,  event.getMessage().getAdapter());
            }
            else
            {
                msgAdapter = connector.getMessageAdapter(result);
            }
            MuleMessage message = new DefaultMuleMessage(msgAdapter);
            
            //Close or return connection if not in a transaction
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


}
