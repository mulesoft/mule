/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.sqlstrategy;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.transport.jdbc.JdbcUtils;
import org.mule.util.ArrayUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Implements strategy for handling normal select statements + acks.
 */
public  class SelectSqlStatementStrategy implements SqlStatementStrategy
{
    protected transient Logger logger = Logger.getLogger(getClass());

    @Override
    public MuleMessage executeStatement(JdbcConnector connector, ImmutableEndpoint endpoint,
                                        MuleEvent event, long timeout, Connection connection) throws Exception
    {
        logger.debug("Trying to receive a message with a timeout of " + timeout);

        String[] stmts = connector.getReadAndAckStatements(endpoint);

        //Unparsed SQL statements (with #[foo] parameters)
        String readStmt = stmts[0];
        String ackStmt = stmts[1];

        //Storage for params (format is #[foo])
        List<String> readParams = new ArrayList<String>();
        List<String> ackParams = new ArrayList<String>();

        //Prepared statement form (with ? placeholders instead of #[foo] params)
        readStmt = connector.parseStatement(readStmt, readParams);
        ackStmt = connector.parseStatement(ackStmt, ackParams);

        long t0 = System.currentTimeMillis();

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
            result = connector.getQueryRunnerFor(endpoint).query(connection, readStmt,
                connector.getResultSetHandler(), params);

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
                JdbcUtils.rollbackAndClose(connection);
                return null;
            }
        } while (true);

        //Execute ack statement
        if (ackStmt != null)
        {
            Object[] params = connector.getParams(endpoint, ackParams,
                    new DefaultMuleMessage(result, (Map)null, endpoint.getMuleContext()), ackStmt);
            if (logger.isDebugEnabled())
            {
                logger.debug("SQL UPDATE: " + ackStmt + ", params = " + ArrayUtils.toString(params));
            }
            int nbRows = connector.getQueryRunnerFor(endpoint).update(connection, ackStmt, params);
            if (nbRows != 1)
            {
                logger.warn("Row count for ack should be 1 and not " + nbRows);
            }
        }

        // Package up result
        MuleMessage message = null;
        if (event != null)
        {
            message = new DefaultMuleMessage(result, event.getMessage(), endpoint.getMuleContext());
        }
        else
        {
            message = new DefaultMuleMessage(result, endpoint.getMuleContext());
        }
        return message;
    }
}
