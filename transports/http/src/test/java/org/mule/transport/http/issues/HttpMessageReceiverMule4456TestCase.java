/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpMessageReceiverMule4456TestCase extends AbstractServiceAndFlowTestCase
{
    private static final String MESSAGE = "test message";

    private HttpClient httpClient;
    private MuleClient muleClient;

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public HttpMessageReceiverMule4456TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-receiver-mule4456-config-service.xml"},
            {ConfigVariant.FLOW, "http-receiver-mule4456-config-flow.xml"}
        });
    }

    @Override
    protected boolean isGracefulShutdown()
    {
        return true;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        HttpClientParams params = new HttpClientParams();
        params.setVersion(HttpVersion.HTTP_1_1);
        httpClient = new HttpClient(params);
        muleClient = muleContext.getClient();
    }

    @Test
    public void testAsyncPost() throws Exception
    {
        FunctionalTestComponent component = getFunctionalTestComponent("AsyncService");
        component.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object comp) throws Exception
            {
                Thread.sleep(200);
                context.getMessageAsString();
            }
        });

        PostMethod request = new PostMethod("http://localhost:" + dynamicPort1.getNumber());
        RequestEntity entity = new StringRequestEntity(MESSAGE, "text/plain",
            muleContext.getConfiguration().getDefaultEncoding());
        request.setRequestEntity(entity);
        httpClient.executeMethod(request);

        MuleMessage message = muleClient.request("vm://out", 1000);
        assertNotNull(message);
        assertEquals(MESSAGE, message.getPayloadAsString());
    }

    @Test
    public void testAsyncPostWithPersistentSedaQueue() throws Exception
    {
        FunctionalTestComponent component = getFunctionalTestComponent("AsyncPersistentQueueService");
        component.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object comp) throws Exception
            {
                Thread.sleep(200);
                context.getMessageAsString();
            }
        });

        PostMethod request = new PostMethod("http://localhost:" + dynamicPort2.getNumber());
        RequestEntity entity = new StringRequestEntity(MESSAGE, "text/plain", muleContext.getConfiguration()
            .getDefaultEncoding());
        request.setRequestEntity(entity);

        httpClient.executeMethod(request);
        MuleMessage message = muleClient.request("vm://out", 1000);
        assertNotNull(message);
        assertEquals(MESSAGE, message.getPayloadAsString());
    }
}
