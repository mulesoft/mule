/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleContext;
import org.mule.exception.AbstractSystemExceptionStrategy;
import org.mule.tck.listener.FlowExecutionListener;
import org.mule.transport.ConnectException;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

/**
 * Verify that some Connect Exceptions-like are being handled and result in reconnections.
 */
public class FtpReconnectionTestCase extends AbstractFtpServerTestCase
{
    protected TestSystemExceptionStrategy tryReconnectionStrategy;

    public FtpReconnectionTestCase (ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "ftp-reconnection.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        tryReconnectionStrategy = new TestSystemExceptionStrategy(muleContext);
        muleContext.setExceptionListener(tryReconnectionStrategy);

        createFtpServerDir("lostConnection");
    }

    @Test
    public void testLostConnection() throws Exception
    {
        try
        {
            FlowExecutionListener flowExecutionListener = new FlowExecutionListener("LostConnection", muleContext);
            Thread.sleep(1000);
            stopServer();
            flowExecutionListener.waitUntilFlowIsComplete();
            fail("Should be reconnecting forerver");
        }
        catch (java.lang.AssertionError e)
        {
            assertTrue(tryReconnectionStrategy.reconnect);
        }
    }

    @Test
    public void testNoConnection() throws Exception
    {
        try
        {
            stopServer();
            FlowExecutionListener flowExecutionListener = new FlowExecutionListener("LostConnection", muleContext);
            flowExecutionListener.waitUntilFlowIsComplete();
            fail("Should be reconnecting forerver");
        }
        catch (java.lang.AssertionError e)
        {
            assertTrue(tryReconnectionStrategy.reconnect);
        }
    }

    protected class TestSystemExceptionStrategy extends AbstractSystemExceptionStrategy
    {
        public boolean reconnect = false;
        public TestSystemExceptionStrategy(MuleContext muleContext)
        {
            super(muleContext);
        }

        protected void handleReconnection(ConnectException ex)
        {
            reconnect = true;
        }
    }
}
