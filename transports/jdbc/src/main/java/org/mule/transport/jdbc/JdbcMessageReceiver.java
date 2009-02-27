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
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.TransactedPollingMessageReceiver;
import org.mule.transport.jdbc.i18n.JdbcMessages;
import org.mule.util.ArrayUtils;
import org.mule.util.MapUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** TODO */
public class JdbcMessageReceiver extends TransactedPollingMessageReceiver
{

    public static final String RECEIVE_MESSAGE_IN_TRANSCTION = "receiveMessageInTransaction";
    public static final String RECEIVE_MESSAGES_IN_XA_TRANSCTION = "receiveMessagesInXaTransaction";
    
    protected JdbcConnector connector;
    protected String readStmt;
    protected String ackStmt;
    protected List readParams;
    protected List ackParams;
    public boolean receiveMessagesInXaTransaction = false;
    private volatile boolean aggregateResult;
    
    public JdbcMessageReceiver(Connector connector,
                               Service service,
                               InboundEndpoint endpoint,
                               String readStmt,
                               String ackStmt) throws CreateException
    {
        super(connector, service, endpoint);
        this.setFrequency(((JdbcConnector) connector).getPollingFrequency());

        boolean transactedEndpoint = endpoint.getTransactionConfig().isTransacted();
        boolean xaTransactedEndpoint = (transactedEndpoint &&
            endpoint.getTransactionConfig().getFactory() instanceof XaTransactionFactory);
        
        boolean receiveMessageInTransaction = MapUtils.getBooleanValue(endpoint.getProperties(),
            RECEIVE_MESSAGE_IN_TRANSCTION, false);
        this.setReceiveMessagesInTransaction(receiveMessageInTransaction && transactedEndpoint);
        if (receiveMessageInTransaction && !transactedEndpoint)
        {
            logger.warn(JdbcMessages.forcePropertyNoTransaction(RECEIVE_MESSAGE_IN_TRANSCTION, "transaction"));
            receiveMessageInTransaction = false;
        }
        
        receiveMessagesInXaTransaction = MapUtils.getBooleanValue(endpoint.getProperties(),
            RECEIVE_MESSAGES_IN_XA_TRANSCTION, false);
        if (receiveMessagesInXaTransaction && !receiveMessageInTransaction)
        {
            logger.warn(JdbcMessages.forceProperty(RECEIVE_MESSAGES_IN_XA_TRANSCTION, RECEIVE_MESSAGE_IN_TRANSCTION));
            receiveMessagesInXaTransaction = false;
        }
        else if (receiveMessagesInXaTransaction && isReceiveMessagesInTransaction() && !xaTransactedEndpoint)
        {
            logger.warn(JdbcMessages.forcePropertyNoTransaction(RECEIVE_MESSAGES_IN_XA_TRANSCTION, "XA transaction"));
            receiveMessagesInXaTransaction = false;
        }
    
        
        this.connector = (JdbcConnector) connector;
        this.setReceiveMessagesInTransaction(endpoint.getTransactionConfig().isTransacted()
            && !this.connector.isTransactionPerMessage());
        
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
        // template method
    }

    protected void doDisconnect() throws Exception
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
            MuleMessage muleMessage = new DefaultMuleMessage(msgAdapter);
            if (this.ackStmt != null)
            {
                if (aggregateResult)
                {
                    List rows = (List) message;
                    Object[][] paramValuesArray = new Object[rows.size()][];

                    HashMap record;
                    for (int i = 0; i <  rows.size(); i++)
                    {
                        record = (HashMap) rows.get(i);
                        paramValuesArray[i] = connector.getParams(endpoint, this.ackParams, new DefaultMuleMessage(record), this.endpoint.getEndpointURI().getAddress());
                    }
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("SQL UPDATE: " + ackStmt + ", params = " + ArrayUtils.toString(ackParams));
                    }
                    int[] nbRows = connector.getQueryRunner().batch(con, this.ackStmt, paramValuesArray);
                    if (nbRows[0] == 0)
                    {
                        logger.warn(".ack statement did not update any rows");
                    }
                    // Reset this flag
                    aggregateResult = false;
                }
                else
                {
                    Object[] paramValues = connector.getParams(endpoint, this.ackParams, muleMessage, this.endpoint.getEndpointURI().getAddress());
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("SQL UPDATE: " + ackStmt + ", params = " + ArrayUtils.toString(paramValues));
                    }
                    int nbRows = connector.getQueryRunner().update(con, this.ackStmt, paramValues);
                    if (nbRows == 0)
                    {
                        logger.warn(".ack statement did not update any rows");
                    }
                }
            }
            routeMessage(muleMessage, tx, tx != null || endpoint.isSynchronous());

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
            con = this.connector.getConnection();

            Object[] readParams = connector.getParams(endpoint, this.readParams, null, this.endpoint.getEndpointURI().getAddress());
            if (logger.isDebugEnabled())
            {
                logger.debug("SQL QUERY: " + readStmt + ", params = " + ArrayUtils.toString(readParams));
            }
            Object results = connector.getQueryRunner().query(con, this.readStmt, readParams,
                    connector.getResultSetHandler());

            List resultList = (List) results;
            if (resultList != null && resultList.size() > 1 && isReceiveMessagesInTransaction() && !receiveMessagesInXaTransaction)
            {
                aggregateResult = true;
                logger.warn(JdbcMessages.moreThanOneMessageInTransaction(RECEIVE_MESSAGE_IN_TRANSCTION, RECEIVE_MESSAGES_IN_XA_TRANSCTION));
                List singleResultList = new ArrayList(1);
                singleResultList.add(resultList);
                return singleResultList;
            }
            
            return resultList;
        }
        finally
        {
            if (TransactionCoordination.getInstance().getTransaction() == null)
            {
                JdbcUtils.close(con);
            }
        }
    }

}
