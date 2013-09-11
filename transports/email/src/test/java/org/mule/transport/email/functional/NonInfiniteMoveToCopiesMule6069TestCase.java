/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.AbstractConnector;
import org.mule.transport.email.GreenMailUtilities;

public class NonInfiniteMoveToCopiesMule6069TestCase extends
        AbstractEmailFunctionalTestCase {
    
    static protected final String imapConnectorName = "imapConnector";
    static private int numberOfMadePolls;
    protected AbstractConnector imapConnector;
    protected RetrieveMessageReceiverPollCounter receiverPollCounter;
    
    public NonInfiniteMoveToCopiesMule6069TestCase(ConfigVariant variant,
            String configResources) {
        super(variant, STRING_MESSAGE, "imap", configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters() 
    {
        return Arrays.asList(new Object[][] {
                { ConfigVariant.FLOW, "NonInfiniteMoveToCopiesMule6069.xml" } });
    }

    @Test
    public void testRequest() throws Exception
    {
        doRequest();
    }
    
    @Override
    public void doRequest() throws Exception 
    {
        int expectedMailCount = 4;
        imapConnector = (AbstractConnector) muleContext.getRegistry().lookupConnector(imapConnectorName);
        addUnreadMail();
        receiverPollCounter = (RetrieveMessageReceiverPollCounter) 
                imapConnector.getReceivers().values().iterator().next();
        numberOfMadePolls = receiverPollCounter.getNumberOfMadePolls();
        waitForPolls();
        assertEquals("Incorrect amount of mails in mail server using moveToFolder", 
                expectedMailCount, server.getReceivedMessages().length);
    }

    private void addUnreadMail() 
    {
        try
        {
            GreenMailUtilities.storeEmail(server.getManagers().getUserManager(),
                    DEFAULT_EMAIL, DEFAULT_USER, DEFAULT_PASSWORD,
                    GreenMailUtilities.toMessage(DEFAULT_MESSAGE, DEFAULT_EMAIL, null));
        } catch (Exception exception) {
            throw new AssertionError("Mail could not be added");
        }

    }

    private void waitForPolls() 
    {
        Prober prober = new PollingProber(10000, 100);
        prober.check(new Probe()
        {
            private int requiredPollAttempts = numberOfMadePolls+3; // +3 should be safe
            @Override
            public boolean isSatisfied()
            {
                return receiverPollCounter.pollNumbersGreaterThan(requiredPollAttempts);
            }
            @Override
            public String describeFailure()
            {
                return "Poller did not poll more than "+ requiredPollAttempts +"  times";
            }
        });
    }
}
