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
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageAdapter;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.ConnectException;
import org.mule.transport.TransactedPollingMessageReceiver;
import org.mule.util.ArrayUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** TODO */
public class JdbcMessageReceiver extends TransactedPollingMessageReceiver
{

    protected JdbcConnector connector;
    protected String readStmt;
    protected String ackStmt;
    protected List readParams;
    protected List ackParams;

    public JdbcMessageReceiver(Connector connector,
                               Service service,
                               InboundEndpoint endpoint,
                               String readStmt,
                               String ackStmt) throws CreateException
    {
        super(connector, service, endpoint);
        this.setFrequency(((JdbcConnector) connector).getPollingFrequency());
        this.setReceiveMessagesInTransaction(false);

        this.connector = (JdbcConnector) connector;
        this.readParams = new ArrayList();
        this.readStmt = this.connector.parseStatement(readStmt, this.readParams);
        this.ackParams = new ArrayList();
        this.ackStmt = this.connector.parseStatement(ackStmt, this.ackParams);
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        Connection con = null;
        try
        {
            con = this.connector.getConnection();
        }
        catch (Exception e)
        {
            throw new ConnectException(e, this);
        }
        finally
        {
            JdbcUtils.close(con);
        }
    }

    protected void doDisconnect() throws ConnectException
    {
        // noop
    }

    public void processMessage(Object message) throws Exception
    {
        Connection con = null;
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        try
        {
            con = this.connector.getConnection();
            MessageAdapter msgAdapter = this.connector.getMessageAdapter(message);
            MuleMessage umoMessage = new DefaultMuleMessage(msgAdapter);
            if (this.ackStmt != null)
            {
                Object[] ackParams = connector.getParams(endpoint, this.ackParams, umoMessage, this.endpoint.getEndpointURI().getAddress());
                if (logger.isDebugEnabled())
                {
                    logger.debug("SQL UPDATE: " + ackStmt + ", params = " + ArrayUtils.toString(ackParams));
                }
                int nbRows = connector.getQueryRunner().update(con, this.ackStmt, ackParams);
                if (nbRows != 1)
                {
                    logger.warn("Row count for ack should be 1 and not " + nbRows);
                }
            }
            routeMessage(umoMessage, tx, tx != null || endpoint.isSynchronous());

        }
        catch (Exception ex)
        {
            if (tx != null)
            {
                tx.setRollbackOnly();
            }

            // rethrow
            throw ex;
        }
        finally
        {
            if (endpoint.getMuleContext().getTransactionManager() != null || tx == null)
            {
                // We are running in an XA transaction.
                // This call is required here for compatibility with strict XA
                // DataSources
                // implementations, as is the case for WebSphere AS and Weblogic.
                // Failure to do it here may result in a connection leak.
                // The close() call will NOT close the connection, neither will it
                // return it to the pool.
                // It will notify the XA driver's ConnectionEventListener that the XA
                // connection
                // is no longer used by the application and is ready for the 2PC
                // commit.
                JdbcUtils.close(con);
            }
        }
    }

    public List getMessages() throws Exception
    {
        Connection con = null;
        try
        {
            try
            {
                con = this.connector.getConnection();
            }
            catch (SQLException e)
            {
                throw new ConnectException(e, this);
            }

            Object[] readParams = connector.getParams(endpoint, this.readParams, null, this.endpoint.getEndpointURI().getAddress());
            if (logger.isDebugEnabled())
            {
                logger.debug("SQL QUERY: " + readStmt + ", params = " + ArrayUtils.toString(readParams));
            }
            Object results = connector.getQueryRunner().query(con, this.readStmt, readParams,
                    connector.getResultSetHandler());
            return (List) results;
        }
        finally
        {
            JdbcUtils.close(con);
        }
    }

}
