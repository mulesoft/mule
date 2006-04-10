/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jdbc;

import org.mule.MuleManager;
import org.mule.impl.MuleMessage;
import org.mule.providers.ConnectException;
import org.mule.providers.TransactedPollingMessageReceiver;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcMessageReceiver extends TransactedPollingMessageReceiver
{

    protected JdbcConnector connector;
    protected String readStmt;
    protected String ackStmt;
    protected List readParams;
    protected List ackParams;

    public JdbcMessageReceiver(UMOConnector connector,
                               UMOComponent component,
                               UMOEndpoint endpoint,
                               String readStmt,
                               String ackStmt) throws InitialisationException
    {
        super(connector, component, endpoint, new Long(((JdbcConnector) connector).getPollingFrequency()));

        this.receiveMessagesInTransaction = false;
        this.connector = (JdbcConnector) connector;

        this.readParams = new ArrayList();
        this.readStmt = JdbcUtils.parseStatement(readStmt, this.readParams);
        this.ackParams = new ArrayList();
        this.ackStmt = JdbcUtils.parseStatement(ackStmt, this.ackParams);
    }

    public void doConnect() throws Exception
    {
    	Connection con = null;
        try {
            con = this.connector.getConnection();
        } catch (Exception e) {
            throw new ConnectException(e, this);
        } finally {
        	JdbcUtils.close(con);
        }
    }

    public void doDisconnect() throws ConnectException
    {
        // noop
    }

    public void processMessage(Object message) throws Exception
    {
        Connection con = null;
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        try {
            con = this.connector.getConnection();

            if (this.ackStmt != null) {
                Object[] ackParams = JdbcUtils.getParams(endpoint, this.ackParams, message);
                int nbRows = connector.createQueryRunner().update(con, this.ackStmt, ackParams);
                if (nbRows != 1) {
                    logger.warn("Row count for ack should be 1 and not " + nbRows);
                }
            }
            UMOMessageAdapter msgAdapter = this.connector.getMessageAdapter(message);
            UMOMessage umoMessage = new MuleMessage(msgAdapter);
            routeMessage(umoMessage, tx, tx != null || endpoint.isSynchronous());

        } catch(Exception ex) {
            if (tx != null) {
                tx.setRollbackOnly();
            }

            // rethrow
            throw ex;
        } finally {
            if (MuleManager.getInstance().getTransactionManager() != null || tx==null) {
                // We are running in an XA transaction.
                // This call is required here for compatibility with strict XA DataSources
                // implementations, as is the case for WebSphere AS and Weblogic.
                // Failure to do it here may result in a connection leak.
                // The close() call will NOT close the connection, neither will it return it to the pool.
                // It will notify the XA driver's ConnectionEventListener that the XA connection
                // is no longer used by the application and is ready for the 2PC commit.
                JdbcUtils.close(con);
            }
        }
    }

    public List getMessages() throws Exception
    {
        Connection con = null;
        try {
            try {
                con = this.connector.getConnection();
            } catch (SQLException e) {
                throw new ConnectException(e, this);
            }

            Object[] readParams = JdbcUtils.getParams(endpoint, this.readParams, null);
            Object results = connector.createQueryRunner().query(con, this.readStmt, readParams, connector.createResultSetHandler());
            return (List) results;
        } finally {
            JdbcUtils.close(con);
        }
    }

}
