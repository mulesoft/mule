/*
 * $Id$ 
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

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.umo.UMOEventContext;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import java.net.URI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (Rødgrød), 57 = \u06f7\u06f5 in Arabic";
    protected static String TEST_CONTENT_TYPE = "text/plain";
    protected static String TEST_CHARSET = "UTF-8";

    private HttpConnection cnn;
    private PostMethod postMethod;
    
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

    protected void sendTestData(int iterations) throws Exception
    {
        URI uri = getInDest().getUri();
        postMethod = new PostMethod(uri.toString());
        postMethod.setRequestEntity(new StringRequestEntity(TEST_MESSAGE, TEST_CONTENT_TYPE, TEST_CHARSET));
        cnn = new HttpConnection(uri.getHost(), uri.getPort(), Protocol.getProtocol(uri.getScheme()));
        cnn.open();
        postMethod.execute(new HttpState(), cnn);
    }

    protected void receiveAndTestResults() throws Exception
    {
        String msg = postMethod.getResponseBodyAsString();
        assertNotNull(msg);
        assertEquals(TEST_MESSAGE + " Received", msg);
    }

    protected void doTearDown() throws Exception
    {
        if (cnn != null) {
            cnn.close();
        }
    }    
    
    public EventCallback createEventCallback() {
        final EventCallback superCallback = super.createEventCallback();

        return new EventCallback() {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception {
                superCallback.eventReceived(context, Component);
                context.getMessage().setProperty(HttpConstants.HEADER_CONTENT_TYPE, getExpectedContentType());
            }
        };
    }

    protected String getExpectedContentType() {
        return "text/plain;charset=UTF-8";
    }
}
