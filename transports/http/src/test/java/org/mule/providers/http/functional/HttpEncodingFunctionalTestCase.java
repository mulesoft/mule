/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.functional;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:jesper@selskabet.org">Jesper Steen Møller</a>
 * @version $Revision$
 */
public class HttpEncodingFunctionalTestCase extends HttpFunctionalTestCase
{
    UMOMessage reply;

    protected void sendTestData(int iterations) throws Exception
    {
        reply = send(getInDest().getAddress(), TEST_MESSAGE, "text/plain;charset=UTF-8");
    }

    protected void receiveAndTestResults() throws Exception
    {
        assertNotNull(reply);
        assertEquals("200", reply.getProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        assertEquals(TEST_MESSAGE + " Received", reply.getPayloadAsString());
        assertTrue(reply.getProperty(HttpConstants.HEADER_CONTENT_TYPE).toString().startsWith("text/baz"));
    }

    protected String getExpectedContentType()
    {
        return "text/baz;charset=UTF-16BE";
    }

    public UMOMessage send(String url, Object payload, String contentType) throws Exception
    {
        Map messageProperties = new HashMap();
        messageProperties.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "true");
        messageProperties.put(HttpConstants.HEADER_CONTENT_TYPE, contentType);
        UMOMessage message = new MuleMessage(payload, messageProperties);

        UMOEvent event = getEvent(message, url, true, false);
        event.setTimeout(UMOEvent.TIMEOUT_NOT_SET_VALUE);
        return event.getSession().sendEvent(event);
    }

    protected UMOEvent getEvent(UMOMessage message, String uri, boolean synchronous, boolean streaming)
        throws UMOException
    {
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(uri,
            UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        if (!endpoint.getConnector().isStarted() && manager.isStarted())
        {
            endpoint.getConnector().startConnector();
        }
        endpoint.setStreaming(streaming);
        try
        {
            MuleSession session = new MuleSession(message,
                ((AbstractConnector)endpoint.getConnector()).getSessionHandler());
            MuleEvent event = new MuleEvent(message, endpoint, session, synchronous);
            return event;
        }
        catch (Exception e)
        {
            throw new DispatchException(new Message(Messages.FAILED_TO_CREATE_X, "Client event"), message,
                endpoint, e);
        }
    }

}
