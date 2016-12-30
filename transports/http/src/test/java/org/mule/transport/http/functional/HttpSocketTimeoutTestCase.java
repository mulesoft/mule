/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.concurrent.Latch;

public class HttpSocketTimeoutTestCase extends FunctionalTestCase {

    private static final int LATCH_TIMEOUT = 300;
    private static final int POLL_TIMEOUT = 600;
    private static final int POLL_DELAY = 150;

    @Rule
    public DynamicPort port = new DynamicPort("port");

    private static Latch latch;
    private static boolean timedOut;

    @Override
    protected String getConfigFile() {
        return "http-socket-timeout-config.xml";
    }

    @Before
    public void setUp() {
        latch = new Latch();
        timedOut = false;
    }

    @Test
    public void usesSoTimeoutIfAvailable() throws Exception {
        MuleMessage message = runFlow("timeout", TEST_MESSAGE).getMessage();
        assertNotNull(message);
        assertNotNull(message.getPayload());
    }

    @Test
    public void usesResponseTimeoutByDefault() throws Exception {
        runFlow("noTimeout", TEST_MESSAGE).getMessage();
        new PollingProber(POLL_TIMEOUT, POLL_DELAY).check(new Probe() {

            @Override
            public boolean isSatisfied()
            {
                return timedOut;
            }

            @Override
            public String describeFailure()
            {
                return "HTTP request should have timed out.";
            }
        });
    }

    protected static class WaitFailureProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                timedOut = latch.await(LATCH_TIMEOUT, MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                // Do nothing
            }
            return event;
        }
    }

    protected static class ReleaseLatchProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            latch.release();
            return event;
        }
    }
}
