/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static javax.mail.Flags.Flag.SEEN;
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

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AbstractImapRetrieveMessagesTestCase extends AbstractEmailFunctionalTestCase
{

    private static final int UNREAD_MESSAGES = 10;
    private static final int READ_MESSAGES_LESS_THAN_BATCH_SIZE = 4;
    protected static final int READ_MESSAGES_GREATER_THAN_BATCH_SIZE = 8;
    protected static final Collection<Object[]> READ_MESSAGES_PARAMETERS = Arrays.asList(new Object[][] {
            {READ_MESSAGES_LESS_THAN_BATCH_SIZE}, {READ_MESSAGES_GREATER_THAN_BATCH_SIZE}});

    public static final int POLL_DELAY_MILLIS = 1000;
    public static final int TIMEOUT_MILLIS = 10000;

    protected static Set<Object> retrievedMessages;

    private FlowExecutionListener flowExecutionListener;
    public int initialReadMessages;

    public AbstractImapRetrieveMessagesTestCase(ConfigVariant variant, String configResources, int initialReadMessages)
    {
        super(variant, STRING_MESSAGE, "imap", configResources);
        this.initialReadMessages = initialReadMessages;
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
        assertThat(server.getReceivedMessages().length, is(equalTo(UNREAD_MESSAGES + initialReadMessages)));

        flowExecutionListener.waitUntilFlowIsComplete();

        Prober prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return (retrievedMessages.size() == UNREAD_MESSAGES);
            }

            @Override
            public String describeFailure()
            {
                return "Expected email count was " + UNREAD_MESSAGES + " but actual one was " + retrievedMessages.size();
            }
        });

        for (int i = 0; i < UNREAD_MESSAGES; i++)
        {
            assertThat("Missing email " + i, retrievedMessages.contains(String.valueOf(i)), is(equalTo(true)));
        }
    }

    @Override
    protected void generateAndStoreEmail() throws Exception
    {
        // Add an initial amount of read emails in the server.
        generateAndStoreUnreadEmails(initialReadMessages);
        GreenMailUser user = server.getManagers().getUserManager().getUser(DEFAULT_USER);
        MailFolder folder = server.getManagers().getImapHostManager().getInbox(user);
        for (long messageId : folder.getMessageUids())
        {
            folder.setFlags(new Flags(SEEN), true, messageId, null, false);
        }

        // Add unread messages.
        generateAndStoreUnreadEmails(UNREAD_MESSAGES);
    }

    private void generateAndStoreUnreadEmails(int count) throws Exception
    {
        List<MimeMessage> messages = new ArrayList<MimeMessage>();
        for (int i = 0; i < count; i++)
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
