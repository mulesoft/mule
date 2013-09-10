/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleEventContext;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.Callable;
import org.mule.construct.Flow;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.transport.email.GreenMailUtilities;
import org.mule.transport.email.ImapConnector;
import org.mule.transport.email.RetrieveMessageReceiver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runners.Parameterized;

/**
 * Tests the correct undeployment of an application with an IMAP inbound endpoint. This test is related to MULE-6737.
 */
public class ImapUndeployTestCase extends AbstractEmailFunctionalTestCase
{

    private static final int NUMBER_OF_MESSAGES = 2;
    private static int numberOfProcessedMessages = 0;

    private static final CountDownLatch disposeLatch = new CountDownLatch(1);
    private static final CountDownLatch mailProcessorLatch = new CountDownLatch(1);

    public ImapUndeployTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "imap", configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "imap-undeploy-test.xml"}
        });
    }

    protected void generateAndStoreEmail() throws Exception
    {
        // Generate messages.
        List<MimeMessage> messages = new ArrayList<MimeMessage>();
        for (int i = 0; i < NUMBER_OF_MESSAGES; i++)
        {
            messages.add(GreenMailUtilities.toMessage(DEFAULT_MESSAGE, DEFAULT_EMAIL, null));
        }
        storeEmail(messages);
    }

    @Test
    public void undeployImapApplication() throws Exception
    {
        /* Get the receiver so later we can check the stopping condition. This needs to be done before starting to
         * dispose because the receiver is removed from the connector. */
        final RetrieveMessageReceiver receiver = getReceiver();

        // Wait for first message to be processed by the MailMessageProcessor.
        disposeLatch.await();

        // Launch a new thread "t" to dispose the context.
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                muleContext.dispose();
            }
        };
        t.start();

        // Wait until "t" has begun to stop.
        new PollingProber(10000, 100).check(new Probe()
        {
            public boolean isSatisfied()
            {
                return (receiver.isStopping() || receiver.isStopped());
            }

            public String describeFailure()
            {
                return null;
            }
        });
        // Now that the context is stopping, allow message processing to go on.
        mailProcessorLatch.countDown();

        // Assert that only one message has been procesed.
        assertEquals(1, numberOfProcessedMessages);

        // Wait until context is disposed.
        t.join();
    }

    /**
     * Retrieves the IMAP message receiver from the context.
     *
     * @return The IMAP message receiver.
     */
    private RetrieveMessageReceiver getReceiver()
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("emailPollingFlow");
        InboundEndpoint inboundEndpoint = (InboundEndpoint) flow.getMessageSource();
        ImapConnector connector = (ImapConnector) inboundEndpoint.getConnector();
        return (RetrieveMessageReceiver) connector.getReceiver(flow, inboundEndpoint);
    }

    /**
     * Custom component to help synchronize the disposing process.
     */
    public static class MailMessageProcessor implements Callable
    {
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            numberOfProcessedMessages++;
            // Allow thread "t" to dispose.
            disposeLatch.countDown();
            // Wait until mule context has stopped.
            mailProcessorLatch.await();
            return eventContext.getMessage();
        }
    }
}
