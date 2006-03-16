/*
 * $Header$ 
 * $Revision$ 
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers.http.functional;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractConnector;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:jesper@selskabet.org">Jesper Steen Møller</a>
 * @version $Revision$
 */
public class MuleHttpFunctionalTestCase extends HttpFunctionalTestCase
{
    UMOMessage reply;

    protected UMOEndpointURI getInDest()
    {
        try {
            return new MuleEndpointURI("http://localhost:60198");
        }
        catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        return null;
    }

    protected UMOConnector createConnector() throws Exception
    {
        HttpConnector connector = new HttpConnector();
        connector.setName("testHttp");
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        return connector;
    }
	
    protected void sendTestData(int iterations) throws Exception {
    	try {
    		reply = send("http://localhost:60198", TEST_MESSAGE);
    	} catch (Exception e) {
            URI uri = getInDest().getUri();
            reply = send(uri.toString(), TEST_MESSAGE);
    	}
    }
    protected void receiveAndTestResults() throws Exception
    {
        assertNotNull(reply);
        assertEquals(TEST_MESSAGE + " Received", reply.getPayloadAsString());
        assertTrue(reply.getProperty(HttpConstants.HEADER_CONTENT_TYPE).toString().startsWith("text/baz"));
    }

    protected String getExpectedContentType() {
    	return "text/baz;charset=UTF-16BE";
    }
    
    public UMOMessage send(String url, Object payload) throws Exception {
        Map messageProperties = new HashMap();
        messageProperties.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "true");
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE,
                "text/plain;charset=UTF-7");
        UMOMessage message = new MuleMessage(payload, messageProperties);

        UMOEvent event = getEvent(message, url, true, false);
        event.setTimeout(UMOEvent.TIMEOUT_NOT_SET_VALUE);
        return event.getSession().sendEvent(event);
    }

    protected UMOEvent getEvent(UMOMessage message, String uri, boolean synchronous, boolean streaming) throws UMOException
    {
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(uri, UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        if (!endpoint.getConnector().isStarted() && manager.isStarted()) {
            endpoint.getConnector().startConnector();
        }
        endpoint.setStreaming(streaming);
        try {
            MuleSession session = new MuleSession(message, ((AbstractConnector)endpoint.getConnector()).getSessionHandler());
            MuleEvent event = new MuleEvent(message, endpoint, session, synchronous);
            return event;
        } catch (Exception e) {
            throw new DispatchException(new Message(Messages.FAILED_TO_CREATE_X, "Client event"), message, endpoint, e);
        }
    }
    
}
