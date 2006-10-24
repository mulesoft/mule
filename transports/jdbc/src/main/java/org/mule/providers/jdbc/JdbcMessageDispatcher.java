/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.lang.StringUtils;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.ConnectorException;
import org.mule.umo.provider.UMOMessageAdapter;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * The Jdbc Message dispatcher is responsible for executing SQL queries against a
 * database.
 */
public class JdbcMessageDispatcher extends AbstractMessageDispatcher
{

    private JdbcConnector connector;

    public JdbcMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JdbcConnector)endpoint.getConnector();
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

        UMOImmutableEndpoint endpoint = event.getEndpoint();
        String writeStmt = endpoint.getEndpointURI().getAddress();
        String str;
        if ((str = this.connector.getQuery(endpoint, writeStmt)) != null)
        {
            writeStmt = str;
        }
        if (StringUtils.isBlank(writeStmt))
        {
            throw new IllegalArgumentException("Missing a write statement");
        }
        if (!"insert".equalsIgnoreCase(writeStmt.substring(0, 6))
            && !"update".equalsIgnoreCase(writeStmt.substring(0, 6))
            && !"delete".equalsIgnoreCase(writeStmt.substring(0, 6)))
        {
            throw new IllegalArgumentException(
                "Write statement should be an insert / update / delete sql statement");
        }
        List paramNames = new ArrayList();
        writeStmt = connector.parseStatement(writeStmt, paramNames);

        Object[] paramValues = connector.getParams(endpoint, paramNames, new MuleMessage(
            event.getTransformedMessage()));

        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        Connection con = null;
        try
        {
            con = this.connector.getConnection();

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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.AbstractMessageDispatcher#doSend(org.mule.umo.UMOEvent)
     */
    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Trying to receive a message with a timeout of " + timeout);
        }

        String[] stmts = this.connector.getReadAndAckStatements(endpoint);
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
            con = this.connector.getConnection();
            if (timeout < 0)
            {
                timeout = Long.MAX_VALUE;
            }
            Object result;
            do
            {
                result = connector.createQueryRunner().query(con, readStmt,
                    connector.getParams(endpoint, readParams, null), new MapHandler());
                if (result != null)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Received: " + result);
                    }
                    break;
                }
                long sleep = Math.min(this.connector.getPollingFrequency(),
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
                    return null;
                }
            }
            while (true);
            if (ackStmt != null)
            {
                int nbRows = connector.createQueryRunner().update(con, ackStmt,
                    connector.getParams(endpoint, ackParams, result));
                if (nbRows != 1)
                {
                    logger.warn("Row count for ack should be 1 and not " + nbRows);
                }
            }
            UMOMessageAdapter msgAdapter = this.connector.getMessageAdapter(result);
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

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageDispatcher#getDelegateSession()
     */
    public Object getDelegateSession() throws UMOException
    {
        try
        {
            return connector.getConnection();
        }
        catch (Exception e)
        {
            throw new ConnectorException(new Message(Messages.FAILED_TO_CREATE_X, "Jdbc Connection"),
                connector, e);
        }
    }

}
