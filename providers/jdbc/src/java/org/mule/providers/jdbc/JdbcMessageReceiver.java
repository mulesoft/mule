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
import java.util.Iterator;
import java.util.List;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.mule.InitialisationException;
import org.mule.MuleManager;
import org.mule.impl.MuleMessage;
import org.mule.providers.PollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcMessageReceiver extends PollingMessageReceiver {

	private JdbcConnector connector;
	private String readStmt;
	private String ackStmt;
	private List readParams;
	private List ackParams;
	
	public JdbcMessageReceiver(UMOConnector connector,
                               UMOComponent component,
                               UMOEndpoint endpoint,
                               String readStmt,
                               String ackStmt) throws InitialisationException {
        super(connector, component, endpoint, new Long(((JdbcConnector) connector).getPollingFrequency()));
        this.connector = (JdbcConnector) connector;

        this.readParams = new ArrayList();
        this.readStmt = JdbcConnector.parseStatement(readStmt, this.readParams);
        this.ackParams = new ArrayList();
        this.ackStmt = JdbcConnector.parseStatement(ackStmt, this.ackParams);

		if (this.connector.getDataSource() instanceof XADataSource) {
			TransactionManager transactionManager = MuleManager.getInstance().getTransactionManager();
			if (transactionManager == null) {
				throw new InitialisationException("A transaction manager must be set on mule manager");
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.mule.providers.PollingMessageReceiver#poll()
	 */
	synchronized public void poll() {
		try {
			List results = getResults();
			for (Iterator it = results.iterator(); it.hasNext();) {
				Object session = null;
				UMOTransaction tx = null;
				Connection con = null;
				Object result = it.next();
				try {
					session = this.connector.getSession(endpoint);
					if (session instanceof Connection) {
						con = (Connection) session;
					} else {
						con = ((XAConnection) session).getConnection();
					}
					if (this.connector.getDataSource() instanceof XADataSource) {
						TransactionManager transactionManager = MuleManager.getInstance().getTransactionManager();
				        while (transactionManager.getTransaction() != null) {
				            Thread.sleep(100);
				        }
				        logger.debug("starting Xa Transaction");
				        transactionManager.begin();
					}
					tx = this.connector.beginTransaction(endpoint, session);
					if (this.ackStmt != null) {
						Object[] ackParams = JdbcConnector.getParams(getEndpointURI(), this.ackParams, result);
						int nbRows = new QueryRunner().update(con, this.ackStmt, ackParams);
						if (nbRows != 1) {
							logger.warn("Row count for ack should be 1 and not " + nbRows);
						}
					}
		            UMOMessageAdapter msgAdapter = this.connector.getMessageAdapter(result);
		            UMOMessage message = new MuleMessage(msgAdapter);
		            routeMessage(message, tx, tx != null || this.connector.isSynchronous());
		            if (tx == null) {
		            	DbUtils.commitAndClose(con);
		            }
				} catch (Exception e) {
					rollback(tx, con);
					throw e;
				}
			}
		} catch (Exception e) {
            logger.error(e);
            try {
                endpoint.getConnector().stop();
            }
            catch (Exception e2) {
                logger.error("Failed to stop endpoint: " + e2.getMessage(), e2);
            }
		}
		
	}
	
	protected List getResults() throws Exception {
		Connection con = null;
		try {
			Object session = this.connector.getSession(endpoint);
			if (session instanceof Connection) {
				con = (Connection) session;
			} else {
				con = ((XAConnection) session).getConnection();
			}
			Object[] readParams = JdbcConnector.getParams(getEndpointURI(), this.readParams, null);
			Object results = new QueryRunner().query(con, this.readStmt, readParams, new MapListHandler());
			return (List) results;
		} finally {
			DbUtils.close(con);
		}
	}
	
	protected void rollback(UMOTransaction tx, Connection con) throws Exception {
		if (tx != null) {
			tx.rollback();
		} else {
			DbUtils.rollback(con);
		}
		if (con != null) {
			DbUtils.close(con);
		}
	}

}
