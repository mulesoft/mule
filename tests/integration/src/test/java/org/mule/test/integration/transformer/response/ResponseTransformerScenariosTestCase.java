/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transformer.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

public class ResponseTransformerScenariosTestCase extends FunctionalTestCase
{
    private static String VM_INBOUND = " inbound";
    private static String VM_OUTBOUND = " outbound";
    private static String VM_RESPONSE = " response";

    private static String VM_OUT_IN_RESP = VM_OUTBOUND + VM_INBOUND + VM_RESPONSE;

    private static String CUSTOM_RESPONSE = " customResponse";

    public ResponseTransformerScenariosTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transformer/response/response-transformer-scenarios.xml";
    }

    // ***** RESPONSE ENDPONTS ON INBOUND ENDPOINTS USED FOR SYNC RESPONSE AFTER ROUTING *****
    // Applied by DefaultInternalMessageListener

    // TODO FAILING MULE- 2969

    // @Test
    //public void testCxfSyncResponseTransformer() throws Exception
    // {
    // MuleClient client = new MuleClient();
    // MuleMessage message = client.send("cxf:http://localhost:4444/services/CxfSync?method=echo",
    // "request",
    // null);
    // assertNotNull(message);
    // assertEquals("request" + "customResponse", message.getPayloadAsString());
    // }

    @Test
    public void testVmSync() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://sync", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUT_IN_RESP, message.getPayloadAsString());
    }

    @Test
    public void testVmSyncResponseTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        // This will disable the transformers configured in the VM connector's service-overrides.
        props.put(MuleProperties.MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY, "true");

        MuleMessage message = client.send("vm://syncResponseTransformer", "request", props);
        assertNotNull(message);
        assertEquals("request" + CUSTOM_RESPONSE, message.getPayloadAsString());
    }

    @Test
    public void testHttpSync() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("http://localhost:4446", "request", null);
        assertNotNull(message);
        // Ensure MuleMessageToHttpResponse was used before sending response

        String server = message.getInboundProperty(HttpConstants.HEADER_SERVER);
        assertTrue(server.startsWith("Mule"));

        String dateStr = message.getInboundProperty(HttpConstants.HEADER_DATE);
        SimpleDateFormat format = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
        Date msgDate = format.parse(dateStr);
        assertTrue(new Date().after(msgDate));

        assertEquals("request", message.getPayloadAsString());
    }

    @Test
    public void testHttpSyncResponseTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("http://localhost:4447", "request", null);
        assertNotNull(message);

        String server = message.getInboundProperty(HttpConstants.HEADER_SERVER);
        assertTrue(server.startsWith("Mule"));

        String dateStr = message.getInboundProperty(HttpConstants.HEADER_DATE);
        SimpleDateFormat format = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
        Date msgDate = format.parse(dateStr);
        assertTrue(new Date().after(msgDate));

        assertEquals("request" + CUSTOM_RESPONSE, message.getPayloadAsString());
    }

    // ***** RESPONSE ENDPONTS ON INBOUND ENDPOINTS USED FOR REMOTE-SYNC RESPONSE AFTER ROUTRING *****
    // Applied by ReplyToHandler

    // DF: The following scenario is no longer supported as from Mule 3.2, a error is logged if response
    // transformers are configured on a jms request-response inbound endpoint.

    // @Test
    // public void testJmsSyncResponseTransformer() throws Exception
    // {
    // MuleClient client = new MuleClient(muleContext);
    // MuleMessage message = client.send("jms://sync", "request", null);
    // assertNotNull(message);
    // assertEquals("request" + CUSTOM_RESPONSE, message.getPayloadAsString());
    // }

    // ***** RESPONSE ENDPONTS ON OUTBOUND ENDPOINT *****
    // Applied by DefaultMuleSession once result message is received from remote endpoint.

    @Test
    public void testVmSyncOutboundEndpointResponseTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://syncOutboundEndpointResponseTransformer", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUTBOUND + VM_INBOUND + VM_OUT_IN_RESP + CUSTOM_RESPONSE + VM_RESPONSE,
            message.getPayloadAsString());
    }

    @Test
    public void testJmsRemoteSync() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://jmsSync", "request", null);
        assertNotNull(message);

        assertEquals("request" + VM_OUT_IN_RESP, message.getPayloadAsString());
    }

    @Test
    public void testJmsSyncOutboundEndpointResponseTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://jmsSyncOutboundEndpointResponseTransformer", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUTBOUND + VM_INBOUND + CUSTOM_RESPONSE + VM_RESPONSE, message.getPayloadAsString());
    }

    @Test
    public void testChainedRouterOutboundEndpointResponseTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://chainedRouterOutboundEndpointResponseTransformer", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUTBOUND + VM_INBOUND + VM_OUT_IN_RESP + VM_OUT_IN_RESP + CUSTOM_RESPONSE
            + CUSTOM_RESPONSE + VM_RESPONSE, message.getPayloadAsString());
    }

    @Test
    public void testNestedRouterOutboundEndpointResponseTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("vm://nestedRouterOutboundEndpointResponseTransformer", "request", null);
        assertNotNull(message);
        assertEquals("request" + VM_OUTBOUND + VM_INBOUND + VM_OUT_IN_RESP + CUSTOM_RESPONSE + CUSTOM_RESPONSE
            + VM_RESPONSE, message.getPayloadAsString());
    }
}
