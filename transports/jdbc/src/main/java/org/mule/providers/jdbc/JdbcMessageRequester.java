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
import org.mule.providers.AbstractMessageRequester;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOMessageAdapter;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * The Jdbc Message dispatcher is responsible for executing SQL queries against a
 * database.
 */
public class JdbcMessageRequester extends AbstractMessageRequester
{

    private JdbcConnector connector;

    public JdbcMessageRequester(UMOImmutableEndpoint endpoint)
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

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was available
     * @throws Exception if the call to the underlying protocol causes an exception
     */
    protected UMOMessage doRequest(long timeout) throws Exception
    {
        return doRequest(timeout, null);
    }

    /**
     * Make a specific request to the underlying transport
     * Special case: The event is need when doReceive was called from doSend
     * @param timeout only for compatibility with doReceive(long timeout)
     * @param event There is a need to get params from message
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was available
     * @throws Exception if the call to the underlying protocol causes an exception
     */
    protected UMOMessage doRequest(long timeout, UMOEvent event) throws Exception
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
                                                             connector.getParams(endpoint,
                                                                                 readParams,
                                                                                 event!=null ? event.getMessage() : null,
                                                                                 this.endpoint.getEndpointURI().getAddress()),
                                                             connector.createResultSetHandler());
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
                                                                  connector.getParams(endpoint, ackParams, result, ackStmt));
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


    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

}