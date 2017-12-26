package org.mule.transport.email.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
                {ConfigVariant.FLOW, "smtp-transport-config-test.xml"}
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
