/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.tck.AbstractServiceAndFlowTestCase.ConfigVariant.FLOW;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.transport.email.SmtpMessageDispatcher;
import org.mule.transport.email.SmtpMessageDispatcherFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

public class SmtpTransportConfigTestCase extends AbstractEmailFunctionalTestCase
{

    private static String user;

    private static final String TEST_USER = "test%40@test.com";

    private static final CountDownLatch latch = new CountDownLatch(1);

    public SmtpTransportConfigTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "smtp", configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {FLOW, "smtp-transport-config-test.xml"}
        });
    }

    @Test
    public void testSend() throws Exception {
        runFlow("endpointTransportConfigFlow");
        latch.await();
        assertThat(user, is(TEST_USER));
    }

    private static class TestSmtpTransportDispatcher extends SmtpMessageDispatcher
    {
        private TestSmtpTransportDispatcher(OutboundEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected void doConnect() throws Exception
        {
            super.doConnect();
            MuleEndpointURI muleEndpointURI = new MuleEndpointURI(transport.getURLName().toString(), muleContext);
            user = muleEndpointURI.getUser();
            latch.countDown();
        }
    }

    public static class TestTransportDispatcherFactory extends SmtpMessageDispatcherFactory
    {
        @Override
        public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
        {
            return new TestSmtpTransportDispatcher(endpoint);
        }
    }

}
