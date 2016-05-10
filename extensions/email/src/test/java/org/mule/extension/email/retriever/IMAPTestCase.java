/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static javax.mail.Flags.Flag;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.RECENT;
import static javax.mail.Flags.Flag.SEEN;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.*;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

@RunWith(Parameterized.class)
public class IMAPTestCase extends AbstractEmailRetrieverTestCase
{

    private static final String RETRIEVE_AND_DONT_READ = "retrieveAndDontRead";
    private static final String RETRIEVE_AND_THEN_EXPUNGE_DELETE = "retrieveAndThenExpungeDelete";
    private static final String RETRIEVE_AND_MARK_AS_DELETE = "retrieveAndMarkDelete";
    private static final String RETRIEVE_AND_MARK_AS_READ = "retrieveAndMarkRead";
    private static final String RETRIEVE_MATCH_NOT_READ = "retrieveOnlyNotReadEmails";
    private static final String RETRIEVE_MATCH_RECENT = "retrieveOnlyRecentEmails";

    @Parameter
    public String protocol;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"imap"}, {"imaps"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return format("retriever/%s.xml", protocol);
    }

    @Override
    public String getProtocol()
    {
        return protocol;
    }

    @Test
    public void retrieveAndRead() throws Exception
    {
        deliver10To(JUANI_EMAIL);
        MuleEvent event = runFlow(RETRIEVE_AND_READ);
        List<MuleMessage> messages = (List<MuleMessage>) event.getMessage().getPayload();
        assertThat(messages, hasSize(10));

        messages.forEach(m -> {
            assertThat(m.getPayload(), instanceOf(String.class));
            assertThat(m.getPayload(), is(EMAIL_CONTENT));
            assertThat(((EmailAttributes) m.getAttributes()).getFlags().isSeen(), is(true));
        });
    }

    @Test
    public void retrieveAndDontRead() throws Exception
    {
        deliver10To(JUANI_EMAIL);
        MuleEvent event = runFlow(RETRIEVE_AND_DONT_READ);
        List<MuleMessage> messages = (List<MuleMessage>) event.getMessage().getPayload();
        assertThat(messages, hasSize(10));
        messages.forEach(m -> assertThat(((EmailAttributes) m.getAttributes()).getFlags().isSeen(), is(false)));
    }

    @Test
    public void retrieveAndThenRead() throws Exception
    {
        deliver10To(JUANI_EMAIL);
        runFlow(RETRIEVE_AND_MARK_AS_READ);
        MimeMessage[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(10));
        for (MimeMessage message : messages)
        {
            assertThat(message.getFlags().contains(SEEN), is(true));
        }
    }

    @Test
    public void retrieveAndMarkAsDelete() throws Exception
    {
        deliver10To(JUANI_EMAIL);

        for (MimeMessage m : server.getReceivedMessages())
        {
            assertThat(m.getFlags().contains(DELETED), is(false));
        }

        runFlow(RETRIEVE_AND_MARK_AS_DELETE);
        assertThat(server.getReceivedMessages().length, is(10));

        for (MimeMessage m : server.getReceivedMessages())
        {
            assertThat(m.getFlags().contains(DELETED), is(true));
        }
    }

    @Test
    public void retrieveOnlyNotRead() throws Exception
    {
        testMatcherFlag(RETRIEVE_MATCH_NOT_READ, SEEN, true);
    }

    @Test
    public void retrieveOnlyRecent() throws Exception
    {
        testMatcherFlag(RETRIEVE_MATCH_RECENT, RECENT, false);
    }

    public void testMatcherFlag(String flowName, Flag flag, boolean flagState) throws Exception
    {
        deliver10To(JUANI_EMAIL);

        for (int i = 0; i < 3; i++)
        {
            MimeMessage message = server.getReceivedMessages()[i];
            message.setFlag(flag, flagState);
        }

        List<MuleMessage> messages = (List<MuleMessage>) runFlow(flowName).getMessage().getPayload();
        assertThat(server.getReceivedMessages().length, is(10));
        assertThat(messages, hasSize(7));
    }

    // TODO: CHECK IF THIS IS POSSIBLE IN POP3.
    @Test
    public void retrieveAndExpungeDelete() throws Exception
    {
        deliver10To(JUANI_EMAIL);

        for (MimeMessage m : server.getReceivedMessages())
        {
            assertThat(m.getFlags().contains(DELETED), is(false));
        }
        runFlow(RETRIEVE_AND_THEN_EXPUNGE_DELETE);
        assertThat(server.getReceivedMessages().length, is(0));
    }

}
