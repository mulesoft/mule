/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.api.config.MuleProperties.MULE_REMOTE_CLIENT_ADDRESS;
import static org.mule.transport.http.HttpConnector.HTTP_CONTEXT_URI_PROPERTY;
import static org.mule.transport.http.HttpConnector.HTTP_METHOD_PROPERTY;
import static org.mule.transport.http.HttpConnector.HTTP_REQUEST_PATH_PROPERTY;
import static org.mule.transport.http.HttpConnector.HTTP_REQUEST_PROPERTY;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.functional.HttpFunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JettyHttpFunctionalTestCase extends HttpFunctionalTestCase
{

    public JettyHttpFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "jetty-http-functional-test-service.xml"},
            {ConfigVariant.FLOW, "jetty-http-functional-test-flow.xml"}
        });
    }      
    
    @Test
    public void testNonRootUrls() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        Map props = new HashMap();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
        MuleMessage result = client.send("anotherClientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    public static class CheckForProperties implements Callable
    {

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            assertThat(eventContext.getMessage().getInboundPropertyNames(), hasItem(HTTP_REQUEST_PROPERTY));
            assertThat(eventContext.getMessage().getInboundPropertyNames(), hasItem(HTTP_REQUEST_PATH_PROPERTY));
            assertThat(eventContext.getMessage().getInboundPropertyNames(), hasItem(MULE_REMOTE_CLIENT_ADDRESS));
            assertThat(eventContext.getMessage().getInboundPropertyNames(), hasItem(HTTP_CONTEXT_URI_PROPERTY));
            assertThat(eventContext.getMessage().getInboundPropertyNames(), hasItem(HTTP_METHOD_PROPERTY));
            return eventContext.getMessage();
        }

    }

}
