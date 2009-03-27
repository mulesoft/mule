/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transformer.response;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpMessageAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResponseTransformerScenariosTestCase extends FunctionalTestCase
{

    private static String VM_INBOUND = " inbound";
    private static String VM_OUTBOUND = " outbound";
    private static String VM_RESPONSE = " response";

    private static String VM_OUT_IN_RESP = VM_OUTBOUND + VM_INBOUND + VM_RESPONSE;

    private static String CUSTOM_RESPONSE = " customResponse";

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transformer/response/response-transformer-scenarios.xml";
    }

    // ***** RESPONSE ENDPONTS ON INBOUND ENDPOINTS USED FOR SYNC RESPONSE AFTER ROUTING *****
    // Applied by DefaultInternalMessageListener

    // TODO FAILING MULE- 2969

    // public void testCxfSyncResponseTransformer() throws Exception
    // {
    // MuleClient client = new MuleClient();
    // MuleMessage message = client.send("cxf:http://localhost:4444/services/CxfSync?method=echo",
    // "request",
    // null);
    // assertNotNull(message);
    // assertEquals("request" + "customResponse", message.getPayloadAsString());
    // }

    // TODO Not sure how to implement with axis

    // public void testAxisSyncResponseTransformer() throws Exception
    // {
    // MuleClient client = new MuleClient();
    // MuleMessage message = client.send("axis:http://localhost:4445/services/AxisSync?method=echo",
    // "request",
    // null);
    // assertNotNull(message);
    // assertEquals("request" + "customResponse", message.getPayloadAsString());
    // }

    public void testVmSync() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://sync", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUT_IN_RESP, message.getPayloadAsString());
    }

    public void testVmSyncResponseTransformer() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://syncResponseTransformer", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUTBOUND + VM_INBOUND + CUSTOM_RESPONSE, message.getPayloadAsString());
    }

    public void testHttpSync() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("http://localhost:4446", "request", null);
        assertNotNull(message);
        // Ensure MuleMessageToHttpResponse was used before sending response

        String server = ((HttpMessageAdapter) message.getAdapter()).getHeader(HttpConstants.HEADER_SERVER).getValue();
        assertTrue(server.startsWith("Mule"));
        
        String dateStr = ((HttpMessageAdapter) message.getAdapter()).getHeader(HttpConstants.HEADER_DATE).getValue();
        SimpleDateFormat format = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
        Date msgDate = format.parse(dateStr);
        assertTrue(new Date().after(msgDate));
        
        assertEquals("request", message.getPayloadAsString());
    }

    public void testHttpSyncResponseTransformer() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("http://localhost:4447", "request", null);
        assertNotNull(message);
        // Ensure MuleMessageToHttpResponse was used before sending response
        // NOTE: With http setting custom response transformer does not replace transport default, becasue
        // custom transformers are called in DefaultInternalMessageListener and http transformer perform
        // explict response transformer afterwards in HttpMessageReciever.doRequest(HttpRequest request,
        // RequestLine requestLine) before returning result to client
        assertTrue(message.getAdapter() instanceof HttpMessageAdapter);

        String server = ((HttpMessageAdapter) message.getAdapter()).getHeader(HttpConstants.HEADER_SERVER).getValue();
        assertTrue(server.startsWith("Mule"));
        
        String dateStr = ((HttpMessageAdapter) message.getAdapter()).getHeader(HttpConstants.HEADER_DATE).getValue();
        SimpleDateFormat format = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
        Date msgDate = format.parse(dateStr);
        assertTrue(new Date().after(msgDate));
        
        assertEquals("request" + CUSTOM_RESPONSE, message.getPayloadAsString());
    }

    // ***** RESPONSE ENDPONTS ON INBOUND ENDPOINTS USED FOR REMOTE-SYNC RESPONSE AFTER ROUTRING *****
    // Applied by ReplyToHandler

    public void testJmsSyncResponseTransformer() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("jms://sync", "request", null);
        assertNotNull(message);
        assertEquals("request" + CUSTOM_RESPONSE, message.getPayloadAsString());
    }

    // ***** RESPONSE ENDPONTS ON OUTBOUND ENDPOINT *****
    // Applied by DefaultMuleSession once result message is received from remote endpoint.

    public void testVmSyncOutboundEndpointResponseTransformer() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://syncOutboundEndpointResponseTransformer", "request", null);
        assertNotNull(message);
         assertEquals("request" + VM_OUTBOUND + VM_INBOUND + VM_OUT_IN_RESP + CUSTOM_RESPONSE + VM_RESPONSE,
         message.getPayloadAsString());
    }

    public void testJmsRemoteSync() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://jmsSync", "request", null);
        assertNotNull(message);

        assertEquals("request" + VM_OUT_IN_RESP, message.getPayloadAsString());
    }

    public void testJmsSyncOutboundEndpointResponseTransformer() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://jmsSyncOutboundEndpointResponseTransformer", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUTBOUND + VM_INBOUND + CUSTOM_RESPONSE + VM_RESPONSE, message.getPayloadAsString());
    }

    public void testChainedRouterOutboundEndpointResponseTransformer() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://chainedRouterOutboundEndpointResponseTransformer", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUTBOUND + VM_INBOUND + VM_OUT_IN_RESP + VM_OUT_IN_RESP + CUSTOM_RESPONSE
                      + VM_RESPONSE, message.getPayloadAsString());
    }

    public void testNestedRouterOutboundEndpointResponseTransformer() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage message = client.send("vm://nestedRouterOutboundEndpointResponseTransformer", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUTBOUND + VM_INBOUND + VM_OUT_IN_RESP + CUSTOM_RESPONSE + CUSTOM_RESPONSE
                     + VM_RESPONSE, message.getPayloadAsString());
    }

}
