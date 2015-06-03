/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transport.Connector;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.TransactedPollingMessageReceiver;
import org.mule.transport.jdbc.i18n.JdbcMessages;
import org.mule.util.ArrayUtils;
import org.mule.util.MapUtils;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements {@link TransactedPollingMessageReceiver} reading data from a database.
 * Provides a way to acknowledge each read data using a SQL statement.
 */
public class JdbcMessageReceiver extends TransactedPollingMessageReceiver
{
    public static final String RECEIVE_MESSAGE_IN_TRANSCTION = "receiveMessageInTransaction";
    public static final String RECEIVE_MESSAGES_IN_XA_TRANSCTION = "receiveMessagesInXaTransaction";

    protected JdbcConnector connector;
    protected String readStmt;
    protected String ackStmt;
    protected List<String> readParams;
    protected List<String> ackParams;
    public boolean receiveMessagesInXaTransaction = false;
    private volatile boolean aggregateResult;

    public JdbcMessageReceiver(Connector connector,
                               FlowConstruct flowConstruct,
                               InboundEndpoint endpoint,
                               String readStmt,
                               String ackStmt) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
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

        parseStatements(readStmt, ackStmt);
    }

    /**
     * Parses the read and acknowledge SQL statements
     */
    protected void parseStatements(String readStmt, String ackStmt)
    {
        this.readParams = new ArrayList<String>();
        this.readStmt = this.connector.parseStatement(readStmt, this.readParams);
        this.ackParams = new ArrayList<String>();
        this.ackStmt = this.connector.parseStatement(ackStmt, this.ackParams);
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // noop
    }

    @Override
    public MuleEvent processMessage(Object message) throws Exception
    {
        Connection con = null;
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        try
        {
            MuleMessage muleMessage = createMuleMessage(message, endpoint.getEncoding());
            MuleEvent result = routeMessage(muleMessage);
            if (hasAckStatement())
            {
                con = this.connector.getConnection();

                if (aggregateResult)
                {
                    List<MuleMessage> messages = createMuleMessages((List) message);
                    int[] nbRows = executeBatchAckStatement(con, messages);

                    if (nbRows[0] == 0)
                    {
                        logger.warn(".ack statement did not update any rows");
                    }
                    // Reset this flag
                    aggregateResult = false;
                }
                else
                {
                    int nbRows = executeAckStatement(con, muleMessage);
                    if (nbRows == 0)
                    {
                        logger.warn(".ack statement did not update any rows");
                    }
                }
            }
            return result;
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
            if (tx == null)
            {
                //Only close connection when there's no transaction.
                //If there's a transaction available, then the transaction
                //will be the one doing close after commit or rollback
                JdbcUtils.close(con);
            }
        }
    }

    protected boolean hasAckStatement()
    {
        return this.ackStmt != null;
    }

    /**
     * Creates a mule message per each data record.
     *
     * @param records data records used to created the payload of the new messages.
     * @return the created messages
     */
    protected List<MuleMessage> createMuleMessages(List<Object> records)
    {
        List<MuleMessage> messages = new LinkedList<MuleMessage>();
        for (Object record : records)
        {
            messages.add(new DefaultMuleMessage(record, getEndpoint().getMuleContext()));
        }

        return messages;
    }

    /**
     * Executes the acknowledge SQL statement for a given message.
     *
     * @param con         database connection to execute the statement
     * @param muleMessage message to been acknowledge
     * @return the number of updated rows by the SQL statement
     * @throws Exception
     */
    protected int executeAckStatement(Connection con, MuleMessage muleMessage)
            throws Exception
    {
        Object[] paramValues = connector.getParams(endpoint, this.ackParams, muleMessage, this.endpoint.getEndpointURI().getAddress());
        if (logger.isDebugEnabled())
        {
            logger.debug("SQL UPDATE: " + ackStmt + ", params = " + ArrayUtils.toString(paramValues));
        }
        int nbRows = connector.getQueryRunnerFor(endpoint).update(con, this.ackStmt, paramValues);
        return nbRows;
    }

    /**
     * Executes the acknowledge SQL statement for a list of messages.
     *
     * @param con      database connection to execute the statement
     * @param messages messages to be acknowledge
     * @return the number of updated rows by each batched execution
     * @throws Exception
     */
    protected int[] executeBatchAckStatement(Connection con, List<MuleMessage> messages)
            throws Exception
    {
        Object[][] paramValuesArray = new Object[messages.size()][];

        for (int i = 0; i < messages.size(); i++)
        {
            MuleMessage message = messages.get(i);
            paramValuesArray[i] = connector.getParams(endpoint, this.ackParams, message, this.endpoint.getEndpointURI().getAddress());
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("SQL UPDATE: " + ackStmt + ", params = " + ArrayUtils.toString(ackParams));
        }

        int[] nbRows = connector.getQueryRunnerFor(endpoint).batch(con, this.ackStmt, paramValuesArray);

        return nbRows;
    }

    @Override
    public List getMessages() throws Exception
    {
        if (!flowConstruct.getMuleContext().isPrimaryPollingInstance())
        {
            return null;
        }
        Connection con = null;
        try
        {
            con = this.connector.getConnection();

            List resultList = executeReadStatement(con);
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

    /**
     * Executes the read SQL statement to get data from the database.
     *
     * @param con database connection to execute the statement
     * @return the list of read records
     * @throws Exception
     */
    protected List executeReadStatement(Connection con) throws Exception
    {
        Object[] readParams = connector.getParams(endpoint, this.readParams, null, this.endpoint.getEndpointURI().getAddress());
        if (logger.isDebugEnabled())
        {
            logger.debug("SQL QUERY: " + readStmt + ", params = " + ArrayUtils.toString(readParams));
        }
        Object results = connector.getQueryRunnerFor(endpoint).query(con, this.readStmt, readParams,
                connector.getResultSetHandler());

        return (List) results;
    }
}
