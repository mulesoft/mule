/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.transport.jms.test.JmsTestContextFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class JmsJndiReconnectionTestCase extends FunctionalTestCase
{


    @Override
    protected String getConfigResources()
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
