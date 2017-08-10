/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.transport.email.SmtpConnector.DEFAULT_SUBJECT_VALUE;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.email.SmtpMessageDispatcher;
import org.mule.transport.email.SmtpMessageDispatcherFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.mail.Address;
import javax.mail.Message;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class SmtpEndpointAttributesTestCase extends AbstractEmailFunctionalTestCase
{

    private static final CountDownLatch latch = new CountDownLatch(3);
    private static final List<Message> processedMessages = new ArrayList<>();
    public SmtpEndpointAttributesTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, STRING_MESSAGE, "smtp", configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "smtp-endpoint-attributes-config.xml"}
        });
    }      
    
    @Test
    public void testSend() throws Exception
    {
        runFlow("endpointAttributesFlow");
        latch.await();
        Message firstMessage = processedMessages.get(0);
        Message secondMessage = processedMessages.get(1);
        Message thirdMessage = processedMessages.get(2);
        assertMessage(firstMessage, "first");
        assertMessage(secondMessage, "second");
        assertMessageWithNullValues(thirdMessage, "third");
    }

    private void assertMessage (Message message, String messageId) throws Exception
    {
        Address[]  ccAddresses = message.getRecipients(Message.RecipientType.CC);
        Address[]  bccAddresses = message.getRecipients(Message.RecipientType.BCC);
        assertThat(message.getContent(), is((Object) (messageId + "-payload")));
        assertThat(ccAddresses[0].toString(), is (messageId + "-cc@example.com"));
        assertThat(bccAddresses[0].toString(), is(messageId + "-bcc@example.com" ));
        assertThat(message.getReplyTo()[0].toString(), is(messageId + "-reply-to@example.com"));
        assertThat(message.getSubject(), is(messageId + "-subject"));
    }

    private void assertMessageWithNullValues(Message message, String messageId) throws Exception
    {
        Address[]  ccAddresses = message.getRecipients(Message.RecipientType.CC);
        Address[]  bccAddresses = message.getRecipients(Message.RecipientType.BCC);
        assertThat(message.getContent(), is((Object) (messageId + "-payload")));
        assertThat(ccAddresses, is(nullValue()));
        assertThat(bccAddresses, is(nullValue()));
        assertThat(message.getReplyTo(), is(nullValue()));
        assertThat(message.getSubject(), is(DEFAULT_SUBJECT_VALUE));
    }

    private static class TestSmtpServiceDispatcher extends SmtpMessageDispatcher
    {
        private TestSmtpServiceDispatcher(OutboundEndpoint endpoint)
        {
            super(endpoint);
        }

        @Override
        protected void doDispatch(MuleEvent event) throws Exception
        {
            processedMessages.add((Message) event.getMessage().getPayload());
            latch.countDown();
        }
    }

    public static class TestSmtpServiceDispatcherFactory extends SmtpMessageDispatcherFactory
    {
        @Override
        public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
        {
            return new TestSmtpServiceDispatcher(endpoint);
        }
    }

}
