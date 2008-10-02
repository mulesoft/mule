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
import java.util.List;

/** TODO */
public class JdbcMessageReceiver extends TransactedPollingMessageReceiver
{

    public static final String RECEIVE_MESSAGE_IN_TRANSCTION = "receiveMessageInTransaction";
    public static final String RECEIVE_MESSAGES_IN_XA_TRANSCTION = "receiveMessagesInXaTransaction";
    
    protected JdbcConnector connector;

    private Connection jdbcConnection;
    
    protected String readStmt;
    protected String ackStmt;
    protected List readParams;
    protected List ackParams;
    public boolean receiveMessagesInXaTransaction = false;
    
    /** Are we inside a transaction? */
    private boolean transaction;

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

    public void processMessage(Object message) throws Exception
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        try
        {
            MessageAdapter msgAdapter = this.connector.getMessageAdapter(message);
            MuleMessage umoMessage = new DefaultMuleMessage(msgAdapter);
            if (this.ackStmt != null)
            {
                Object[] ackParams = connector.getParams(endpoint, this.ackParams, umoMessage, this.endpoint.getEndpointURI().getAddress());
                if (logger.isDebugEnabled())
                {
                    logger.debug("SQL UPDATE: " + ackStmt + ", params = " + ArrayUtils.toString(ackParams));
                }
                int nbRows = connector.getQueryRunner().update(jdbcConnection, this.ackStmt, ackParams);
                if (nbRows != 1)
                {
                    logger.warn("Row count for ack should be 1 and not " + nbRows);
                }
            }
            routeMessage(umoMessage, tx, tx != null || endpoint.isSynchronous());

        }
        catch (Exception ex)
        {
            if (tx == null)
            {
                jdbcConnection.rollback();
            }
            else
            {
                tx.setRollbackOnly();
            }
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
                //JdbcUtils.close(con);
            }
        }
    }

    public List getMessages() throws Exception
    {
        Object[] readParams = connector.getParams(endpoint, this.readParams, null, this.endpoint.getEndpointURI().getAddress());
        if (logger.isDebugEnabled())
        {
            logger.debug("SQL QUERY: " + readStmt + ", params = " + ArrayUtils.toString(readParams));
        }
        Object results = 
            connector.getQueryRunner().query(jdbcConnection, this.readStmt, readParams, connector.getResultSetHandler());

        List resultList = (List) results;
        if (resultList != null && resultList.size() > 1 && isReceiveMessagesInTransaction() && !receiveMessagesInXaTransaction)
        {
            logger.warn(JdbcMessages.moreThanOneMessageInTransaction(RECEIVE_MESSAGE_IN_TRANSCTION, RECEIVE_MESSAGES_IN_XA_TRANSCTION));
            List singleResultList = new ArrayList(1);
            singleResultList.add(resultList);
            return singleResultList;
        }
        
        return resultList;
    }
}
