/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.callback.HttpCallback;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.module.http.internal.config.HttpConfiguration;
import org.mule.module.http.internal.listener.DefaultHttpListenerConfig;
import org.mule.security.oauth.DefaultHttpCallback;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import java.util.Arrays;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHttpCallbackTestCase extends AbstractMuleContextTestCase
{

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHttpCallbackTestCase.class);
    private static final String CALLBACK_PATH = "callback";
    private static final String CONNECTOR_HTTP_MULE_DEFAULT = "connector.http.mule.default";

    @Rule
    public DynamicPort localPort = new DynamicPort("localPort");

    @Rule
    public DynamicPort remotePort = new DynamicPort("remotePort");

    @Mock
    private MessageProcessor processor;

    private HttpClient httpClient;

    private GetMethod callbackMethod;

    private HttpCallback callback;

    @Override
    protected void doSetUp() throws Exception
    {
        httpClient = new HttpClient();
        callbackMethod = new GetMethod(buildCallbackUrl());
        when(processor.process(any(MuleEvent.class))).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return (MuleEvent) invocationOnMock.getArguments()[0];
            }
        });
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (callbackMethod != null)
        {
            try
            {
                callbackMethod.releaseConnection();
            }
            catch (Exception e)
            {
                handleExceptionWhileTearingDown(e);
            }
        }

        if (callback != null)
        {
            try
            {
                callback.stop();
            }
            catch (Exception e)
            {
                handleExceptionWhileTearingDown(e);
            }
        }
    }

    @Test
    public void withNewHttpConnector() throws Exception
    {
        muleContext.start();
        HttpListenerConfig config = createListenerConfig();
        callback = createCallback(config);
        sendCallbackRequest();
    }

    @Test
    public void withOldHttpTransport() throws Exception
    {
        // Needs the context to be started so endpoints are available
        muleContext.start();

        callback = createCallback(newOldHttpTransport());
        sendCallbackRequest();
    }

    @Test
    public void withOldHttpTransportWithUnstartedMuleContext() throws Exception
    {
        callback = createCallback(newOldHttpTransport());

        muleContext.start();

        sendCallbackRequest();
    }

    @Test
    public void withNewHttpConnectorByDefault() throws Exception
    {
        muleContext.start();
        createListenerConfig();

        callback = createCallback(null);
        sendCallbackRequest();
    }

    private HttpListenerConfig createListenerConfig() throws MuleException
    {
        DefaultHttpListenerConfig listenerConfig = new DefaultHttpListenerConfig();
        listenerConfig.setPort(localPort.getNumber());
        listenerConfig.setHost("localhost");
        listenerConfig.setMuleContext(muleContext);
        muleContext.getRegistry().registerObject("callbackConfig", listenerConfig);

        listenerConfig.start();

        return listenerConfig;
    }

    @Test
    public void withNewHttpConnectorByDefaultAndNoListenerConfigured() throws Exception
    {
        muleContext.start();
        callback = createCallback(null);
        sendCallbackRequest();
    }

    @Test
    public void withOldHttpTransportByDefault() throws Exception
    {
        muleContext.start();
        MuleTestUtils.testWithSystemProperty(HttpConfiguration.USE_HTTP_TRANSPORT_FOR_URIS, Boolean.TRUE.toString(), new MuleTestUtils.TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                newOldHttpTransport();
                callback = createCallback(null);
                sendCallbackRequest();         }
        });
    }

    private void sendCallbackRequest() throws Exception
    {
        int response = httpClient.executeMethod(callbackMethod);
        assertThat(response, is(HttpStatus.SC_OK));
        verify(processor).process(any(MuleEvent.class));
    }

    private HttpCallback createCallback(Object connector) throws MuleException
    {
        HttpCallback callback = new DefaultHttpCallback(Arrays.asList(processor),
                                                        muleContext,
                                                        "localhost",
                                                        localPort.getNumber(),
                                                        remotePort.getNumber(),
                                                        CALLBACK_PATH,
                                                        false,
                                                        connector);
        callback.start();
        return callback;
    }

    private HttpConnector newOldHttpTransport() throws Exception
    {
        HttpConnector httpConnector = new HttpConnector(muleContext);
        httpConnector.initialise();
        httpConnector.start();

        muleContext.getRegistry().registerObject(CONNECTOR_HTTP_MULE_DEFAULT, httpConnector);

        return httpConnector;
    }

    private void handleExceptionWhileTearingDown(Throwable t)
    {
        LOGGER.error("Found exception at tear down", t);
    }

    private String buildCallbackUrl()
    {
        return String.format("http://localhost:%d/%s", localPort.getNumber(), CALLBACK_PATH);
    }

    @Override
    protected boolean isDisposeContextPerClass() {
        return false;
    }

}
