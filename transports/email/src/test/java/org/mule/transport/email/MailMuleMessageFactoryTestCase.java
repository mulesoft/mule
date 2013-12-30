/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import java.util.Date;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.junit.Test;

public class MailMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{

    protected static final String TEST_TO = "Information <info@domain.com>";
    protected static final String TEST_CC = "\"info@\" <domain.com info@domain.com>";
    protected static final String TEST_BCC = "'invalid@domain.com', info <info@domain.com>";
    protected static final String TEST_FROM = "me@myco.com";
    protected static final String TEST_SUBJECT = "eMail Subject";
    protected static final String TEST_REPLY_TO = "reply@myco.com";
    protected static final String TEST_CONTENT_TYPE = "text/plain";


    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new MailMuleMessageFactory();
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
        MuleMessage muleMessage = factory.create(payload, encoding, muleContext);
        assertEquals(2, muleMessage.getInboundAttachmentNames().size());
    }

    @Test
    public void testAddRecipientProperties() throws Exception
    {
        Date now = new Date();
        
        MimeMessage payload = getValidTransportMessage();
        payload.setHeader(RecipientType.TO.toString(), TEST_TO);
        payload.setHeader(RecipientType.CC.toString(), TEST_CC);
        payload.setHeader(RecipientType.BCC.toString(), TEST_BCC);
        payload.setFrom(new InternetAddress(TEST_FROM));
        payload.setReplyTo(new InternetAddress[]{new InternetAddress(TEST_REPLY_TO)});
        payload.setSentDate(now);

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage muleMessage = factory.create(payload, encoding, muleContext);

        assertEquals(TEST_TO, muleMessage.getInboundProperty(MailProperties.TO_ADDRESSES_PROPERTY));
        assertEquals(TEST_CC, muleMessage.getInboundProperty(MailProperties.CC_ADDRESSES_PROPERTY));
        assertEquals(TEST_BCC, muleMessage.getInboundProperty(MailProperties.BCC_ADDRESSES_PROPERTY));
        assertEquals(TEST_FROM, muleMessage.getInboundProperty(MailProperties.FROM_ADDRESS_PROPERTY));
        assertEquals(TEST_REPLY_TO, muleMessage.getInboundProperty(MailProperties.REPLY_TO_ADDRESSES_PROPERTY));
        assertEquals("(no subject)", muleMessage.getInboundProperty(MailProperties.SUBJECT_PROPERTY));
        assertEquals(TEST_CONTENT_TYPE, muleMessage.getInboundProperty(MailProperties.CONTENT_TYPE_PROPERTY));
        assertEquals(new MailDateFormat().parse(new MailDateFormat().format(now)), muleMessage.getInboundProperty(MailProperties.SENT_DATE_PROPERTY));
    }

    @Test
    public void testAddMailHeadersToMessageProperties() throws Exception
    {
        String invalid = "Invalid";
        String customProperty = "customProperty";
        Date now = new Date();

        MimeMessage payload = getValidTransportMessage();
        payload.setHeader(RecipientType.TO.toString(), TEST_TO);
        payload.setHeader(MailProperties.TO_ADDRESSES_PROPERTY,invalid);
        payload.setHeader(RecipientType.CC.toString(), TEST_CC);
        payload.setHeader(MailProperties.CC_ADDRESSES_PROPERTY,invalid);
        payload.setHeader(RecipientType.BCC.toString(), TEST_BCC);
        payload.setHeader(MailProperties.BCC_ADDRESSES_PROPERTY,invalid);
        payload.setFrom(new InternetAddress(TEST_FROM));
        payload.setHeader(MailProperties.FROM_ADDRESS_PROPERTY, invalid);
        payload.setReplyTo(new InternetAddress[] {new InternetAddress(TEST_REPLY_TO)});
        payload.setHeader(MailProperties.REPLY_TO_ADDRESSES_PROPERTY, invalid);
        payload.setSentDate(now);
        payload.setHeader(MailProperties.SENT_DATE_PROPERTY, invalid);
        payload.setSubject(TEST_SUBJECT);
        payload.setHeader(MailProperties.CONTENT_TYPE_PROPERTY, invalid);
        payload.setHeader(customProperty, customProperty);

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage muleMessage = factory.create(payload, encoding, muleContext);

        assertEquals(TEST_TO, muleMessage.getInboundProperty(MailProperties.TO_ADDRESSES_PROPERTY));
        assertEquals(TEST_CC, muleMessage.getInboundProperty(MailProperties.CC_ADDRESSES_PROPERTY));
        assertEquals(TEST_BCC, muleMessage.getInboundProperty(MailProperties.BCC_ADDRESSES_PROPERTY));
        assertEquals(TEST_FROM, muleMessage.getInboundProperty(MailProperties.FROM_ADDRESS_PROPERTY));
        assertEquals(TEST_REPLY_TO, muleMessage.getInboundProperty(MailProperties.REPLY_TO_ADDRESSES_PROPERTY));
        assertEquals(TEST_SUBJECT, muleMessage.getInboundProperty(MailProperties.SUBJECT_PROPERTY));
        assertEquals(TEST_CONTENT_TYPE, muleMessage.getInboundProperty(MailProperties.CONTENT_TYPE_PROPERTY));
        assertEquals(new MailDateFormat().parse(new MailDateFormat().format(now)), muleMessage.getInboundProperty(MailProperties.SENT_DATE_PROPERTY));
        assertEquals(customProperty, muleMessage.getInboundProperty(customProperty));
    }

    @Test
    public void testAttachmentsWithSameName() throws Exception
    {
        Message payload = createMimeMessageWithSameAttachmentNames();

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage muleMessage = factory.create(payload, encoding, muleContext);
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
