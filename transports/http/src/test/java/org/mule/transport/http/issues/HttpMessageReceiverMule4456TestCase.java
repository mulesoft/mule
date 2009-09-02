/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.issues;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;

public class HttpMessageReceiverMule4456TestCase extends FunctionalTestCase
{
    private static final String MESSAGE = "test message";

    private HttpClient httpClient;
    private MuleClient muleClient;

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
        muleClient = new MuleClient();
    }

    @Override
    protected String getConfigResources()
    {
        return "http-receiver-mule4456-config.xml";
    }

    public void testAsyncPost() throws Exception
    {
        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("AsyncService");
        component.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                Thread.sleep(200);
                context.getMessageAsString();
            }
        });

        PostMethod request = new PostMethod("http://localhost:8000");
        RequestEntity entity = new StringRequestEntity(MESSAGE, "text/plain", muleContext.getConfiguration()
            .getDefaultEncoding());
        request.setRequestEntity(entity);

        httpClient.executeMethod(request);
        MuleMessage message = muleClient.request("vm://out", 1000);
        assertNotNull(message);
        assertEquals(MESSAGE, message.getPayloadAsString());
    }

    public void testAsyncPostWithPersistentSedaQueue() throws Exception
    {
        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("AsyncPersistentQueueService");
        component.setEventCallback(new EventCallback()
        {
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                Thread.sleep(200);
                context.getMessageAsString();
            }
        });

        PostMethod request = new PostMethod("http://localhost:8001");
        RequestEntity entity = new StringRequestEntity(MESSAGE, "text/plain", muleContext.getConfiguration()
            .getDefaultEncoding());
        request.setRequestEntity(entity);

        httpClient.executeMethod(request);
        MuleMessage message = muleClient.request("vm://out", 1000);
        assertNotNull(message);
        assertEquals(MESSAGE, message.getPayloadAsString());
    }
}
