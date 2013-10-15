/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MailMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new MailMuleMessageFactory(muleContext);
    }

    @Override
    protected MimeMessage getValidTransportMessage() throws Exception
    {
        MimeMessage message = new MimeMessage((Session) null);
        message.setContent(TEST_MESSAGE, "text/plain; charset=ISO-8859-1");
        return message;
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is not a valid transport message for MailMuleMessageFactory";
    }

    @Test
    public void testAttachments() throws Exception
    {
        Message payload = createMimeMessageWithAttachment();

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage muleMessage = factory.create(payload, encoding);
        assertEquals(2, muleMessage.getInboundAttachmentNames().size());
    }

    @Test
    public void testAddRecipientProperties() throws Exception
    {
        String to = "Information <info@domain.com>";
        String cc = "\"info@\" <domain.com info@domain.com>";
        String bcc = "'invalid@domain.com', info <info@domain.com>";

        MimeMessage payload = getValidTransportMessage();
        payload.setHeader(RecipientType.TO.toString(), to);
        payload.setHeader(RecipientType.CC.toString(), cc);
        payload.setHeader(RecipientType.BCC.toString(), bcc);

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage muleMessage = factory.create(payload, encoding);

        assertEquals(to, muleMessage.getOutboundProperty(MailProperties.INBOUND_TO_ADDRESSES_PROPERTY));
        assertEquals(cc, muleMessage.getOutboundProperty(MailProperties.INBOUND_CC_ADDRESSES_PROPERTY));
        assertEquals(bcc, muleMessage.getOutboundProperty(MailProperties.INBOUND_BCC_ADDRESSES_PROPERTY));
    }

    @Test
    public void testAttachmentsWithSameName() throws Exception
    {
        Message payload = createMimeMessageWithSameAttachmentNames();

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage muleMessage = factory.create(payload, encoding);
        assertEquals(3, muleMessage.getInboundAttachmentNames().size());
    }

    private Message createMimeMessageWithAttachment() throws Exception
    {
        MimeBodyPart mainBody = new MimeBodyPart();
        mainBody.setText("This is the main message text");

        MimeBodyPart attachment = createBodyPart(TEST_MESSAGE, "message.txt");

        Multipart multipart = createMultipart(mainBody, attachment);

        MimeMessage message = getValidTransportMessage();
        message.setContent(multipart);
        return message;
    }

    private Message createMimeMessageWithSameAttachmentNames() throws Exception
    {
        MimeBodyPart mainBody = new MimeBodyPart();
        mainBody.setText("This is the main message text");

        MimeBodyPart firstAttachment = createBodyPart("The first attachment content", "message.txt");
        MimeBodyPart secondAttachment = createBodyPart("The second attachment content", "message.txt");

        Multipart multipart = createMultipart(mainBody, firstAttachment, secondAttachment);

        MimeMessage message = getValidTransportMessage();
        message.setContent(multipart);
        return message;
    }

    private MimeBodyPart createBodyPart(String content, String fileName) throws MessagingException
    {
        MimeBodyPart attachment = new MimeBodyPart();
        DataSource source = new ByteArrayDataSource(content.getBytes(), "text/plain");
        attachment.setDataHandler(new DataHandler(source));
        attachment.setFileName(fileName);
        return attachment;
    }

    private Multipart createMultipart(BodyPart... parts) throws MessagingException
    {
        Multipart multipart = new MimeMultipart();

        for (BodyPart part : parts)
        {
            multipart.addBodyPart(part);
        }

        return multipart;
    }
}
