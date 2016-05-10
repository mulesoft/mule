/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.sender;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.mule.extension.email.internal.builder.EmailAttributesBuilder.fromMessage;
import static org.mule.extension.email.internal.commands.ReplyCommand.NO_EMAIL_FOUND;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getTextBody;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.MG_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.createDefaultMimeMessageBuilder;
import org.mule.extension.email.EmailConnectorTestCase;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.internal.exception.EmailException;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.internal.matchers.StartsWith;

@RunWith(Parameterized.class)
public class SMTPTestCase extends EmailConnectorTestCase
{

    private static final String SEND_EMAIL = "sendEmail";
    private static final String SEND_EMAIL_WITH_ATTACHMENT = "sendEmailWithAttachment";
    private static final String FORWARD_EMAIL = "forwardEmail";
    private static final String REPLY_EMAIL = "replyEmail";

    @Parameter
    public String protocol;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"smtp"}, {"smtps"}
        });
    }


    @Override
    protected String getConfigFile()
    {
        return format("sender/%s.xml", protocol);
    }

    @Override
    public String getProtocol()
    {
        return protocol;
    }

    @Test
    public void sendEmail() throws Exception
    {
        runFlow(SEND_EMAIL);
        assertThat(server.waitForIncomingEmail(5000, 1), is(true));
        Message[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(1));
        assertThat(messages[0].getSubject(), is(EMAIL_SUBJECT));
        assertThat(getTextBody(messages[0]).trim(), is(EMAIL_CONTENT));
    }

    // TODO: This test fails if the attachment content-type is specified
    @Test
    public void sendEmailWithAttachment() throws Exception
    {
        runFlow(SEND_EMAIL_WITH_ATTACHMENT);
        assertThat(server.waitForIncomingEmail(5000, 4), is(true));
        Message[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(4));

        for (Message message : messages)
        {
            Multipart content = (Multipart)message.getContent();
            assertThat(content.getCount(), is(2));

            Object bodyContent = content.getBodyPart(0).getContent();
            assertThat(bodyContent, is(EMAIL_CONTENT));

            DataHandler dataHandler = content.getBodyPart(1).getDataHandler();
            assertThat(dataHandler.getContent(), instanceOf(InputStream.class));
            assertThat(EMAIL_JSON_ATTACHMENT_CONTENT, is(IOUtils.toString((InputStream) dataHandler.getContent())));
        }
    }

    @Test
    public void replyEmail() throws Exception
    {
        EmailAttributes attributes = fromMessage(createDefaultMimeMessageBuilder(JUANI_EMAIL)
                                                              .replyTo(singletonList(MG_EMAIL))
                                                              .build());
        flowRunner(REPLY_EMAIL)
                .withPayload(EMAIL_CONTENT)
                .withAttributes(attributes)
                .run();

        assertThat(server.waitForIncomingEmail(5000, 1), is(true));
        MimeMessage[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(1));
        assertThat(messages[0].getSubject(), new StartsWith("Re"));
        Address[] recipients = messages[0].getAllRecipients();
        assertThat(recipients.length, is(1));
        assertThat(recipients[0].toString(), is(MG_EMAIL));
    }

    @Test
    public void failReply() throws Exception
    {
        expectedException.expectCause(isA(EmailException.class));
        expectedException.expectMessage(containsString(NO_EMAIL_FOUND));
        runFlow(REPLY_EMAIL);
    }

    @Test
    public void forwardEmail() throws Exception
    {
        EmailAttributes attributes = fromMessage(createDefaultMimeMessageBuilder(JUANI_EMAIL)
                                                         .replyTo(singletonList(MG_EMAIL))
                                                         .build());
        flowRunner(FORWARD_EMAIL)
                .withPayload(EMAIL_CONTENT)
                .withAttributes(attributes)
                .run();

        assertThat(server.waitForIncomingEmail(5000, 1), is(true));
        MimeMessage[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(1));
        assertThat(IOUtils.toString((InputStream) messages[0].getContent()).trim(), is(EMAIL_CONTENT));
    }

}
