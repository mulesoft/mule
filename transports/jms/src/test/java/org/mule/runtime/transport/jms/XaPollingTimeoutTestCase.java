/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.transport.jms;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import javax.jms.MessageConsumer;

import org.junit.Test;

public class XaPollingTimeoutTestCase extends FunctionalTestCase
{

    private static MessageConsumer messageConsumer = mock(MessageConsumer.class);

    @Override
    protected String getConfigFile()
    {
        return "xa-polling-timeout-config.xml";
    }

    @Test
    public void usesConfiguredXaPollingTimeout() throws Exception
    {
        final PollingProber prober = new PollingProber(RECEIVE_TIMEOUT, 100);

        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                try
                {
                    verify(messageConsumer, atLeastOnce()).receive(10000);
                    return true;
                }
                catch (Throwable e)
                {
                    return false;
                }
            }

            @Override
            public String describeFailure()
            {
                return "Message consumer was not invoked using the right timeout";
            }
        });
    }

    public static class TestXaTransactedJmsMessageReceiver extends XaTransactedJmsMessageReceiver
    {

        public TestXaTransactedJmsMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException
        {
            super(connector, flowConstruct, endpoint);
        }

        @Override
        protected MessageConsumer createConsumer() throws Exception
        {
            return messageConsumer;
        }
    }
}
