/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.connectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transport.Connector;
import org.mule.retry.policies.NoRetryPolicyTemplate;
import org.mule.transport.email.ImapConnector;
import org.mule.transport.email.RetrieveMessageReceiver;

import com.icegreen.greenmail.util.ServerSetup;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Simple tests for pulling from an IMAP server.
 */
public class ImapConnectorTestCase extends AbstractReceivingMailConnectorTestCase
{
    public ImapConnectorTestCase()
    {
        super(ServerSetup.PROTOCOL_IMAP);
    }

    @Override
    public Connector createConnector() throws Exception
    {
        ImapConnector connector = new ImapConnector(muleContext);
        connector.setName("ImapConnector");
        connector.setCheckFrequency(POLL_PERIOD_MS);
        connector.setServiceOverrides(newEmailToStringServiceOverrides());
        return connector;
    }

    @Test
    public void receiversConnectionIsDoneWithConnectorsRetryPolicy() throws Exception
    {
        ImapConnector aConnector = new ImapConnector(muleContext);
        String imapConnectorUri = "imap://bob:password@localhost:123";
        InboundEndpoint anEndpoint = getTestInboundEndpoint("in", imapConnectorUri, null, null, null, aConnector);
        RetrieveMessageReceiver aReceiver = new RetrieveMessageReceiver(aConnector, getTestService(), anEndpoint, 100, true, "aFolder");

        final AtomicBoolean retryPolicyWasEnforced = new AtomicBoolean(false);

        RetryPolicyTemplate probeRetryPolicyTemplate = mock(NoRetryPolicyTemplate.class);
        when(probeRetryPolicyTemplate.execute(any(RetryCallback.class), any(WorkManager.class)))
                .thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                retryPolicyWasEnforced.set(true);
                return null;
            }
        });
        aConnector.setRetryPolicyTemplate(probeRetryPolicyTemplate);

        aReceiver.connect();

        assertThat(aReceiver.isConnected(), is(true));

        // Receivers connection was done with retry policy
        verify(probeRetryPolicyTemplate, times(1)).execute(any(RetryCallback.class), any(WorkManager.class));
        assertThat(retryPolicyWasEnforced.get(), is(true));
    }

}
