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
package org.mule.vendor.oracle.aq;

import be.kmi_irm.labo.messaging.mule.AQSessionDelegate;
import oracle.jdbc.xa.client.OracleXADataSource;
import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsQueueConnectionFactory;
import org.mule.InitialisationException;
import org.mule.umo.endpoint.UMOEndpoint;

import javax.jms.*;
import javax.naming.NamingException;
import javax.transaction.xa.XAResource;
import java.sql.SQLException;

/**
 * @author henks
 */
public class OracleAQConnector extends JmsConnector {

	private Connection connection;

	private JmsSupport jmsSupport;
	
	private OracleXADataSource oxds;
	
	private XAResource xaRes;
	
	private String url;
	
	
	private String schema;
	
	private int acknowledgementMode = Session.AUTO_ACKNOWLEDGE;

	private boolean noLocal;

	protected Connection createConnection() throws NamingException,
			JMSException, InitialisationException {
		
		java.sql.Connection conn = null;
		QueueConnection queueConnection = null;
		
		try {
			oxds = new OracleXADataSource();
			
			//String url = "jdbc:oracle:oci:@rmidb";
			oxds.setURL(url);
			oxds.setUser(getUsername();
			oxds.setPassword(getPassword());

			// Get a XA connection to the underlying data source
			javax.sql.XAConnection pc = oxds.getXAConnection();
			xaRes = pc.getXAResource();
			//		 Get the Physical Connections
			conn = pc.getConnection();
			queueConnection = AQjmsQueueConnectionFactory.createQueueConnection(conn);
			queueConnection.start();
					
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw new InitialisationException(e.getMessage());
		}
		
		return queueConnection;
	}

	public void doInitialise() throws InitialisationException {
		super.doInitialise();
		try {
			
			jmsSupport = new JmsOracleAQSupport(this.schema);

			setJmsSupport(jmsSupport);
			
			connection = createConnection();
			
		} catch (Exception e) {
			throw new InitialisationException(
					"Failed to create Jms Connector: " + e.getMessage(), e);
		}
	}
	
	public Object getSession(UMOEndpoint endpoint) throws Exception {
		if (endpoint.getTransactionConfig().getFactory() instanceof JmsClientAcknowledgeTransactionFactory) {
			return getSession(false);
		} else {
			return getSession(endpoint.getTransactionConfig().isTransacted());
		}
	}
	
	Session getSession(boolean transacted) throws JMSException {
		Session session = jmsSupport.createSession(connection, transacted, acknowledgementMode, noLocal);
		
		if (transacted) {
			session = new AQSessionDelegate(session,xaRes);
	
		}
		
		return session;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	
	public String getSchema() {
		return schema;
	}
	public void setSchema(String schema) {
		this.schema = schema;
	}
}