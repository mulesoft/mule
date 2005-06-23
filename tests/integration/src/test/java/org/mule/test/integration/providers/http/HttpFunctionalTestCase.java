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

package org.mule.test.integration.providers.http;

import java.net.URI;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.methods.PostMethod;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.NullPayload;
import org.mule.providers.http.HttpConnector;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    protected static final String TEST_MESSAGE = "Test Http Request";

    private HttpConnection cnn;

    protected UMOEndpointURI getInDest()
    {
        try {
            return new MuleEndpointURI("http://localhost:60198");
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        try {
            return new MuleEndpointURI("http://localhost:60199");
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
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
        PostMethod postMethod = new PostMethod(uri.toString());
        postMethod.setRequestBody(TEST_MESSAGE);
        postMethod.setRequestContentLength(TEST_MESSAGE.length());
        cnn = new HttpConnection(uri.getHost(), uri.getPort());
        postMethod.execute(new HttpState(), cnn);
    }

    protected void receiveAndTestResults() throws Exception
    {
        byte[] buf = new byte[1024 * 4];
        int len = cnn.getResponseInputStream().read(buf);
        if (len < 1) {
            fail("Nothing was sent back in the response");
        }
        String msg = new String(buf, 0, len);

        assertNotNull(msg);
        assertEquals(TEST_MESSAGE + " Received", msg);
    }

    protected String getClientUrl()
    {
        return "http://www.google.com.au";
    }

    public void testClient() throws Exception
    {
        UMOEndpointURI request = new MuleEndpointURI(getClientUrl());
        HttpConnector c = (HttpConnector) createConnector();
        c.initialise();
        UMOMessageDispatcher d = c.getDispatcher(request.getAddress());
        UMOEvent e = getTestEvent(new NullPayload(), new MuleEndpoint("test",
                                                                      request,
                                                                      c,
                                                                      null,
                                                                      UMOEndpoint.ENDPOINT_TYPE_SENDER,
                                                                      0,
                                                                      null));
        UMOMessage m = d.send(e);
        assertNotNull(m);
        assertNotNull(m.getPayload());
        assertTrue(m.getPayloadAsString().indexOf("google") > -1);
    }

    public void testClientWithPath() throws Exception
    {
        UMOEndpointURI request = new MuleEndpointURI("http://www.muleumo.org/docs/apidocs");
        HttpConnector c = (HttpConnector) createConnector();
        c.initialise();
        UMOMessageDispatcher d = c.getDispatcher(request.getAddress());
        UMOEvent e = getTestEvent(new NullPayload(), new MuleEndpoint("test",
                                                                      request,
                                                                      c,
                                                                      null,
                                                                      UMOEndpoint.ENDPOINT_TYPE_SENDER,
                                                                      0,
                                                                      null));
        UMOMessage m = d.send(e);
        assertNotNull(m);
        assertNotNull(m.getPayload());
        assertTrue(m.getPayloadAsString().indexOf("javadoc") > -1);
    }
}
