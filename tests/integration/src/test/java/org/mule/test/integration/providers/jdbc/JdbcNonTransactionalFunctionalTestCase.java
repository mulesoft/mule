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
package org.mule.test.integration.providers.jdbc;

import org.enhydra.jdbc.standard.StandardDataSource;
import org.mule.MuleManager;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.functional.EventCallback;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;

import javax.sql.DataSource;
import java.util.HashMap;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcNonTransactionalFunctionalTestCase extends AbstractJdbcFunctionalTestCase {

    public void testDirectSql() throws Exception {
        //Start the server
        MuleManager.getInstance().start();
        
        MuleEndpointURI muleEndpoint = new MuleEndpointURI("jdbc://?sql=SELECT * FROM TEST");
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(muleEndpoint, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        UMOMessage message;
        
        message = endpoint.getConnector().getDispatcher(muleEndpoint.getAddress()).receive(muleEndpoint, 1000);
        assertNull(message);
        
        execSqlUpdate("INSERT INTO TEST(ID, TYPE, DATA, ACK, RESULT) VALUES (NULL, 1, '" + DEFAULT_MESSAGE + "', NULL, NULL)");
        message = endpoint.getConnector().getDispatcher(muleEndpoint.getAddress()).receive(muleEndpoint, 1000);
        assertNotNull(message);
    }

    public void testSend() throws Exception {
        //Start the server
        MuleManager.getInstance().start();
        
        UMOEndpointURI muleEndpoint = new MuleEndpointURI(DEFAULT_OUT_URI);
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(muleEndpoint, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        UMOMessage message = new MuleMessage(DEFAULT_MESSAGE, null);
        MuleSession session = new MuleSession();
        MuleEvent event = new MuleEvent(message, endpoint, session, true);
        session.dispatchEvent(event);

        Object[] obj2 = execSqlQuery("SELECT DATA FROM TEST WHERE TYPE = 2");
        assertNotNull(obj2);
        assertEquals(1, obj2.length);
        assertEquals(DEFAULT_MESSAGE, obj2[0]);
    }
    
    public void testReceive() throws Exception {
        //Start the server
        MuleManager.getInstance().start();
        
        MuleEndpointURI muleEndpoint = new MuleEndpointURI(DEFAULT_IN_URI);
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(muleEndpoint, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        UMOMessage message;
        
        message = endpoint.getConnector().getDispatcher(muleEndpoint.getAddress()).receive(muleEndpoint, 1000);
        assertNull(message);
        
        execSqlUpdate("INSERT INTO TEST(ID, TYPE, DATA, ACK, RESULT) VALUES (NULL, 1, '" + DEFAULT_MESSAGE + "', NULL, NULL)");
        message = endpoint.getConnector().getDispatcher(muleEndpoint.getAddress()).receive(muleEndpoint, 1000);
        assertNotNull(message);
        
    }
    
    public void testReceiveAndSend() throws Exception
    {
        //Start the server
        initialiseComponent(null);
        MuleManager.getInstance().start();

        execSqlUpdate("INSERT INTO TEST(ID, TYPE, DATA, ACK, RESULT) VALUES (NULL, 1, '" + DEFAULT_MESSAGE + "', NULL, NULL)");

        long t0 = System.currentTimeMillis();
        while (System.currentTimeMillis() - t0 < 20000) {
        	Object[] rs = execSqlQuery("SELECT COUNT(*) FROM TEST WHERE TYPE = 2");
            assertNotNull(rs);
            assertEquals(1, rs.length);
            if (((Number) rs[0]).intValue() > 0) {
            	break;
            }
            Thread.sleep(100);
        }

        Object[] obj2 = execSqlQuery("SELECT DATA FROM TEST WHERE TYPE = 2");
        assertNotNull(obj2);
        assertEquals(1, obj2.length);
        assertEquals(DEFAULT_MESSAGE + " Received", obj2[0]);
    }
    
    public void initialiseComponent(EventCallback callback) throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        HashMap props = new HashMap();
        props.put("eventCallback", callback);
        builder.registerComponent(
        		JdbcFunctionalTestComponent.class.getName(),
                "testComponent", getInDest(), getOutDest(), props);
    }
    
	protected DataSource createDataSource() throws Exception {
		StandardDataSource ds = new StandardDataSource();
		ds.setDriverName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:mem:.");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
	}
}
