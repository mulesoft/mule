/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.listener.FlowExecutionListener;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.email.GreenMailUtilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;

public class AbstractImapRetrieveMessagesTestCase extends AbstractEmailFunctionalTestCase
{

    private static final int NUMBER_OF_MESSAGES = 10;
    public static final int POLL_DELAY_MILLIS = 1000;
    public static final int TIMEOUT_MILLIS = 10000;
    private FlowExecutionListener flowExecutionListener;
    protected static Set<Object> retrievedMessages;

    public AbstractImapRetrieveMessagesTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "imap", configResources);
    }

    @Before
    public void setUp()
    {
        retrievedMessages = new HashSet<Object>();
        flowExecutionListener = new FlowExecutionListener("retrieveEmails", muleContext);
    }

    @Test
    public void testRetrieveEmails() throws Exception
    {
        assertThat(server.getReceivedMessages().length, is(equalTo(NUMBER_OF_MESSAGES)));

        flowExecutionListener.waitUntilFlowIsComplete();

        Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return (retrievedMessages.size() == NUMBER_OF_MESSAGES);
            }

            @Override
            public String describeFailure()
            {
                return "Expected email count was "+ NUMBER_OF_MESSAGES +" but actual one was " + retrievedMessages.size();
            }
        });

        for (int i = 0; i < NUMBER_OF_MESSAGES; i++)
        {
            assertThat("Missing email " + i, retrievedMessages.contains(String.valueOf(i)), is(equalTo(true)));
        }
    }

    @Override
    protected void generateAndStoreEmail() throws Exception
    {
        List<MimeMessage> messages = new ArrayList<MimeMessage>();
        for (int i = 0; i < NUMBER_OF_MESSAGES; i++)
        {
            messages.add(GreenMailUtilities.toMessage(String.valueOf(i), DEFAULT_EMAIL, null));
        }
        storeEmail(messages);
    }

    public static class StoreEmailsProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            retrievedMessages.add(event.getMessage().getPayload());
            return event;
        }
    }
}
