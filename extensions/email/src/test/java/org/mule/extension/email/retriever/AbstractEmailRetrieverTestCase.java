/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email.retriever;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.ESTEBAN_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.assertAttachmentContent;
import static org.mule.extension.email.util.EmailTestUtils.buildMultipartMessage;
import static org.mule.extension.email.util.EmailTestUtils.createDefaultMimeMessageBuilder;
import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.MessagingException;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractEmailRetrieverTestCase extends EmailConnectorTestCase
{

    protected static final String RETRIEVE_AND_READ = "retrieveAndRead";
    protected static final String RETRIEVE_MATCH_SUBJECT_AND_FROM = "retrieveMatchingSubjectAndFromAddress";
    protected static final String RETRIEVE_WITH_ATTACHMENTS = "retrieveWithAttachments";
    protected static final String STORE_MESSAGES = "storeMessages";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public SystemProperty temporaryFolderProperty = new SystemProperty("storePath", temporaryFolder.getRoot().getAbsolutePath());

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        temporaryFolder.delete();
    }

    @Test
    public void retrieveNothing() throws Exception
    {
        assertThat(server.getReceivedMessages().length, is(0));
        MuleEvent event = runFlow(RETRIEVE_AND_READ);
        List<MuleMessage> messages = (List<MuleMessage>) event.getMessage().getPayload();
        assertThat(messages, hasSize(0));
    }

    @Test
    public void retrieveMatchingSubjectAndFromAddress() throws Exception
    {
        deliver10To(JUANI_EMAIL);
        for (int i = 0; i < 5; i++)
        {
            String fromEmail = format("address.%s@enterprise.com", i);
            user.deliver(createDefaultMimeMessageBuilder(ESTEBAN_EMAIL)
                                 .withSubject("Non Matching Subject")
                                 .fromAddresses(fromEmail)
                                 .build());
        }

        List<MuleMessage> messages = (List<MuleMessage>) runFlow(RETRIEVE_MATCH_SUBJECT_AND_FROM).getMessage().getPayload();
        assertThat(server.getReceivedMessages().length, is(15));
        assertThat(messages, hasSize(10));
    }

    @Test
    public void retrieveEmailWithAttachments() throws Exception
    {
        user.deliver(buildMultipartMessage());
        List<MuleMessage> messages = (List<MuleMessage>) runFlow(RETRIEVE_WITH_ATTACHMENTS).getMessage().getPayload();

        assertThat(messages, hasSize(1));
        EmailAttributes attributes = (EmailAttributes) messages.get(0).getAttributes();
        Map<String, DataHandler> emailAttachments = attributes.getAttachments();
        assertThat(emailAttachments.size(), is(2));
        assertThat(emailAttachments.keySet(), containsInAnyOrder(EMAIL_JSON_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME));
        assertAttachmentContent(emailAttachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
        assertAttachmentContent(emailAttachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
    }

    @Test
    public void storeEmailsInDirectory() throws Exception
    {
        // TODO
    }

    @Test
    public void storeSingleEmailInDirectory() throws Exception
    {
        // TODO
    }

    protected void deliver10To(String toEmail) throws MessagingException
    {
        for (int i = 0; i < 10; i++)
        {
            user.deliver(createDefaultMimeMessageBuilder(toEmail).fromAddresses(ESTEBAN_EMAIL).build());
        }
    }
}
