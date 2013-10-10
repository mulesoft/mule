/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import org.mule.api.MuleContext;
import org.mule.api.endpoint.EndpointURI;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class FtpRequestTimeoutTestCase extends AbstractFtpServerTestCase
{

    private static CountDownLatch latch = new CountDownLatch(1);

    public FtpRequestTimeoutTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setStartContext(false);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "ftp-request-timeout-config.xml"}
        });
    }

    @Test
    public void usesResponseTimeout() throws Exception
    {

        createFileOnFtpServer("input.txt");

        final TestSystemExceptionStrategy exceptionListener = new TestSystemExceptionStrategy(muleContext);
        muleContext.setExceptionListener(exceptionListener);
        muleContext.start();

        try
        {
            Prober prober = new PollingProber(RECEIVE_TIMEOUT, 100);
            prober.check(new ExceptionListenerInvokedProbe(exceptionListener));
        }
        finally
        {
            latch.countDown();
        }
    }

    private static class TestSystemExceptionStrategy extends DefaultSystemExceptionStrategy
    {

        private volatile int count = 0;

        public TestSystemExceptionStrategy(MuleContext muleContext)
        {
            super(muleContext);
        }

        @Override
        public void handleException(Exception exception)
        {
            count++;
            super.handleException(exception);
        }

        public int getCount()
        {
            return count;
        }
    }

    public static class TestFtpConnectionFactory extends FtpConnectionFactory
    {

        public TestFtpConnectionFactory(EndpointURI uri)
        {
            super(uri);
        }

        @Override
        protected FTPClient createFtpClient()
        {
            return new TestFTPClient();
        }
    }

    public static class TestFTPClient extends FTPClient
    {

        @Override
        public boolean retrieveFile(String remote, OutputStream local) throws IOException
        {

            try
            {
                latch.wait();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            return false;
        }
    }

    private static class ExceptionListenerInvokedProbe implements Probe
    {

        private final TestSystemExceptionStrategy exceptionListener;

        public ExceptionListenerInvokedProbe(TestSystemExceptionStrategy exceptionListener)
        {
            this.exceptionListener = exceptionListener;
        }

        public boolean isSatisfied()
        {
            return exceptionListener.getCount() > 0;
        }

        public String describeFailure()
        {
            return "Exception listener not invoked";
        }
    }
}


