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
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.jdbc.sqlstrategy.SQLStrategy;

import java.sql.Connection;


public class JdbcMessageRequester extends AbstractMessageRequester
{
    private JdbcConnector connector;

    private Connection jdbcConnection;

    /** Are we inside a transaction? */
    private boolean transaction;

    public JdbcMessageRequester(InboundEndpoint endpoint)
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

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was available
     * @throws Exception if the call to the underlying protocol causes an exception
     */
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        return doRequest(timeout, null);
    }

    /**
     * Make a specific request to the underlying transport
     * Special case: The event is need when doReceive was called from doSend
     * @param timeout only for compatibility with doRequest(long timeout)
     * @param event There is a need to get params from message
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was available
     * @throws Exception if the call to the underlying protocol causes an exception
     */
    protected MuleMessage doRequest(long timeout, MuleEvent event) throws Exception
    {
        SQLStrategy strategy = connector.getSqlStrategyFactory().create("select", null);
        return strategy.executeStatement(jdbcConnection, endpoint, event, timeout);        
    }
}