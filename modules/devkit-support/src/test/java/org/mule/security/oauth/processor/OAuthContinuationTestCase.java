/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.processor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.api.MuleEvent;
import org.mule.api.callback.HttpCallbackFactory;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.processor.MessageProcessor;
import org.mule.security.oauth.DefaultHttpCallback;
import org.mule.security.oauth.callback.DefaultHttpCallbackAdapter;
import org.mule.security.oauth.callback.DefaultHttpCallbackFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConnector;

import org.junit.Rule;
import org.junit.Test;

public class OAuthContinuationTestCase extends AbstractMuleContextTestCase
{

    @Rule
    public DynamicPort localPort = new DynamicPort("localPort");

    @Rule
    public DynamicPort remotePort = new DynamicPort("remotePort");

    private HttpConnector httpConnector;
    private DefaultHttpCallback callback;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        this.httpConnector = new HttpConnector(muleContext);
        this.httpConnector.initialise();
        this.httpConnector.start();
        this.muleContext.start();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (this.callback != null)
        {
            this.callback.stop();
        }
        this.httpConnector.stop();
        this.httpConnector.dispose();
        super.doTearDown();
    }

    @Test
    public void verifyLifecycleOnlyExecutedOnce() throws Exception
    {
        final MuleEvent event = getTestEvent("authCode");

        DefaultHttpCallbackAdapter adapter = new DefaultHttpCallbackAdapter();
        adapter.setDomain("localhost");
        adapter.setLocalPort(this.localPort.getNumber());
        adapter.setRemotePort(this.remotePort.getNumber());
        adapter.setPath("/testCallback");
        adapter.setConnector(this.httpConnector);

        FetchAccessTokenMessageProcessor fetchMessageProcessor = mock(FetchAccessTokenMessageProcessor.class,
            RETURNS_DEEP_STUBS);
        when(fetchMessageProcessor.process(any(MuleEvent.class))).thenReturn(event);

        MessageProcessor listener = mock(MessageProcessor.class,
            withSettings().extraInterfaces(Initialisable.class));
        ((Initialisable) listener).initialise();

        FlowConstruct flowConstruct = mock(FlowConstruct.class);

        HttpCallbackFactory callbackFactory = new DefaultHttpCallbackFactory();
        this.callback = (DefaultHttpCallback) callbackFactory.createCallback(adapter, "(.)",
            fetchMessageProcessor, listener, muleContext, flowConstruct);

        this.callback.start();

        verify(((Initialisable) listener)).initialise();

        this.callback.getFlow().process(event);
        verify(listener).process(event);
    }
}
