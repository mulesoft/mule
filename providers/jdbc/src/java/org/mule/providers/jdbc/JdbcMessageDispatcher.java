/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jdbc;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.XaTransaction;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcMessageDispatcher extends AbstractMessageDispatcher {

    private JdbcConnector connector;

    public JdbcMessageDispatcher(JdbcConnector connector)
    {
        super(connector);
        this.connector = connector;
    }
    
	/* (non-Javadoc)
	 * @see org.mule.providers.AbstractMessageDispatcher#doDispose()
	 */
	public void doDispose() throws UMOException {
	}

	/* (non-Javadoc)
	 * @see org.mule.providers.AbstractMessageDispatcher#doDispatch(org.mule.umo.UMOEvent)
	 */
	public void doDispatch(UMOEvent event) throws Exception {
		logger.debug("Dispatch event: " + event);
		
		UMOEndpoint endpoint = event.getEndpoint();
		UMOEndpointURI endpointURI = endpoint.getEndpointURI();
		String writeStmt = endpointURI.getAddress();
		String str;
		if ((str = (String) this.connector.getQuery(endpoint, writeStmt)) != null) {
    		writeStmt = str;
    	}
		if (writeStmt == null) {
			throw new IllegalArgumentException("Write statement should not be null");
		}
		if (!"insert".equalsIgnoreCase(writeStmt.substring(0, 6)) &&
			!"update".equalsIgnoreCase(writeStmt.substring(0, 6)) &&
			!"delete".equalsIgnoreCase(writeStmt.substring(0, 6))) {
			throw new IllegalArgumentException("Write statement should be an insert / update / delete sql statement");
		}
		List paramNames = new ArrayList();
		writeStmt = JdbcConnector.parseStatement(writeStmt, paramNames);
		Object[] paramValues = JdbcConnector.getParams(endpointURI, paramNames, event.getMessage());
		
		Connection con = null;
		XAConnection xaCon = null;
		UMOTransaction tx = null;
		try {
			QueryRunner runner = new QueryRunner();

	        tx = TransactionCoordination.getInstance().getTransaction();
	        XaTransaction xaTransaction = null;
	        if (tx instanceof XaTransaction) {
	            xaCon = (XAConnection) connector.getSession(endpoint);
				con = xaCon.getConnection();
	            xaTransaction = (XaTransaction) tx;
	            xaTransaction.enlistResource(xaCon.getXAResource());
	        } else if(tx instanceof JdbcTransaction) {
				con = (Connection) tx.getResource();
	        } else if (tx == null) {
				con = (Connection) connector.getSession(endpoint);
	        } else {
	        	throw new IllegalStateException("Unknown transaction running: " + tx.getClass().getName());
	        }
	        
			int nbRows = runner.update(con, writeStmt, paramValues);
			if (nbRows != 1) {
				logger.warn("Row count for write should be 1 and not " + nbRows);
			}
            if (tx == null) {
            	DbUtils.commitAndClose(con);
            } else {
                if (xaTransaction != null) {
                    xaTransaction.delistResource(xaCon.getXAResource(), XAResource.TMSUCCESS);
                }
            	connector.commitTransaction(event);
            }
            logger.debug("Event dispatched succesfuly");
		} catch (Exception e) {
            logger.debug("Error dispatching event: " + e.getMessage(), e);
			if (tx != null) {
				tx.rollback();
			} else {
				DbUtils.rollback(con);
			}
			DbUtils.close(con);
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.providers.AbstractMessageDispatcher#doSend(org.mule.umo.UMOEvent)
	 */
	public UMOMessage doSend(UMOEvent event) throws Exception {
        doDispatch(event);
        return event.getMessage();
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.provider.UMOMessageDispatcher#receive(org.mule.umo.endpoint.UMOEndpointURI, long)
	 */
	public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {

		logger.debug("Trying to receive a message with a timeout of " + timeout);
		
		String[] stmts = this.connector.getReadAndAckStatements(endpointUri, null);
		String readStmt = stmts[0];
		String ackStmt = stmts[1];
        List readParams = new ArrayList();
        List ackParams = new ArrayList();
        readStmt = JdbcConnector.parseStatement(readStmt, readParams);
        ackStmt = JdbcConnector.parseStatement(ackStmt, ackParams);

		Connection con = null;
        QueryRunner runner = new QueryRunner();
		ResultSetHandler handler = new MapHandler();
		long t0 = System.currentTimeMillis();
		try {
			Object ds = this.connector.getDataSource();
			if (ds instanceof XADataSource) {
				con = ((XADataSource) ds).getXAConnection().getConnection();
			} else {
				con = ((DataSource) ds).getConnection();
			}
			if (timeout < 0) {
				timeout = Long.MAX_VALUE;
			}
			Object result = null;
			do {
				result = runner.query(con, readStmt, JdbcConnector.getParams(endpointUri, readParams, null), handler);
				if (result != null) {
					logger.debug("Received: " + result);
					break;
				}
				long sleep = Math.min(this.connector.getPollingFrequency(), timeout - (System.currentTimeMillis() - t0));
				if (sleep > 0) {
					logger.debug("No results, sleeping for " + sleep);
					Thread.sleep(sleep);
				} else {
					logger.debug("Timeout");
					return null;
				}
			} while (true);
			if (result != null && ackStmt != null) {
				int nbRows = runner.update(con, ackStmt, JdbcConnector.getParams(endpointUri, ackParams, result));
				if (nbRows != 1) {
					logger.warn("Row count for ack should be 1 and not " + nbRows);
				}
			}
            UMOMessageAdapter msgAdapter = this.connector.getMessageAdapter(result);
            UMOMessage message = new MuleMessage(msgAdapter);
            DbUtils.commitAndClose(con);
            return message;
		} catch (Exception e) {
			DbUtils.closeQuietly(con);
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.provider.UMOMessageDispatcher#getDelegateSession()
	 */
	public Object getDelegateSession() throws UMOException {
		// TODO Auto-generated method stub
		return null;
	}

}
