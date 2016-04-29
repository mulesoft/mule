/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.transport.jms.JmsConnector;
import org.mule.runtime.transport.jms.test.JmsTestContextFactory;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class JmsJndiReconnectionTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "jms-jndi-reconnection-config.xml";
    }

    @BeforeClass
    public static void makeInitialContextFail()
    {
        JmsTestContextFactory.failWhenRetrievingInitialContext = true;
    }

    @AfterClass
    public static void restoreInitialContextState()
    {
        JmsTestContextFactory.failWhenRetrievingInitialContext = false;
    }

    @Test
    public void testReconnectionWorksWhenInitialContextIsNotAvailable() throws Exception
    {
        JmsTestContextFactory.failWhenRetrievingInitialContext = true;
        try
        {
            final JmsConnector jmsConnector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector");
            assertThat(jmsConnector.isConnected(), is(false));
            JmsTestContextFactory.failWhenRetrievingInitialContext = false;
            PollingProber prober = new PollingProber(RECEIVE_TIMEOUT,100);
            prober.check(new Probe()
            {
                @Override
                public boolean isSatisfied()
                {
                    return jmsConnector.isConnected();
                }

                @Override
                public String describeFailure()
                {
                    return "jms connector should be connected by now.";
                }
            });
        }
        finally
        {
            JmsTestContextFactory.failWhenRetrievingInitialContext = false;
        }
    }
}
