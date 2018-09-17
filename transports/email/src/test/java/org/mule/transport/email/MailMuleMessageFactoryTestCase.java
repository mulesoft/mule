/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.transport.email.MailMuleMessageFactory.DEFAULT_BINARY_CONTENT_TYPE;

import java.util.Date;
import java.util.Properties;

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
import javax.mail.util.SharedByteArrayInputStream;

import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

public class MailMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{

    private static final String BINARY_BIN_FILE = "binary.bin";
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
        assertThat(muleMessage.getInboundAttachmentNames(), hasSize(2));
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

        assertThat((String) muleMessage.getInboundProperty(MailProperties.TO_ADDRESSES_PROPERTY), equalTo(TEST_TO));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.CC_ADDRESSES_PROPERTY), equalTo(TEST_CC));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.BCC_ADDRESSES_PROPERTY), equalTo(TEST_BCC));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.FROM_ADDRESS_PROPERTY), equalTo(TEST_FROM));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.REPLY_TO_ADDRESSES_PROPERTY), equalTo(TEST_REPLY_TO));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.SUBJECT_PROPERTY), equalTo("(no subject)"));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.CONTENT_TYPE_PROPERTY), equalTo(TEST_CONTENT_TYPE));
        assertThat((Date) muleMessage.getInboundProperty(MailProperties.SENT_DATE_PROPERTY), equalTo(new MailDateFormat().parse(new MailDateFormat().format(now))));
    }

    @Test
    public void testAddAttachments() throws Exception
    {
        Date now = new Date();

        MailMuleMessageFactory factory = (MailMuleMessageFactory) createMuleMessageFactory();
        MimeMessage payload = getValidTransportMessage();
        payload.setHeader(RecipientType.TO.toString(), TEST_TO);
        payload.setHeader(RecipientType.CC.toString(), TEST_CC);
        payload.setHeader(RecipientType.BCC.toString(), TEST_BCC);
        payload.setFrom(new InternetAddress(TEST_FROM));
        payload.setReplyTo(new InternetAddress[] {new InternetAddress(TEST_REPLY_TO)});
        payload.setSentDate(now);
        MimeMessage transportMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        DefaultMuleMessage muleMessage = (DefaultMuleMessage) factory.create(payload, encoding, muleContext);
        transportMessage.setFileName(BINARY_BIN_FILE);
        transportMessage.setContent(new SharedByteArrayInputStream(TEST_MESSAGE.getBytes()), DEFAULT_BINARY_CONTENT_TYPE);
        factory.addAttachments(muleMessage, transportMessage);
        DataHandler attachment = muleMessage.getAttachment(BINARY_BIN_FILE);
        assertThat(muleMessage.getInboundAttachmentNames(), hasSize(1));
        assertThat(attachment, is(notNullValue()));
        assertThat(attachment.getContentType(), equalTo(transportMessage.getContentType()));
        assertThat(attachment.getContent().toString(), equalTo(TEST_MESSAGE));
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

        assertThat((String) muleMessage.getInboundProperty(MailProperties.TO_ADDRESSES_PROPERTY), equalTo(TEST_TO));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.CC_ADDRESSES_PROPERTY), equalTo(TEST_CC));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.BCC_ADDRESSES_PROPERTY), equalTo(TEST_BCC));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.FROM_ADDRESS_PROPERTY), equalTo(TEST_FROM));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.REPLY_TO_ADDRESSES_PROPERTY), equalTo(TEST_REPLY_TO));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.SUBJECT_PROPERTY), equalTo(TEST_SUBJECT));
        assertThat((String) muleMessage.getInboundProperty(MailProperties.CONTENT_TYPE_PROPERTY), equalTo(TEST_CONTENT_TYPE));
        assertThat((Date) muleMessage.getInboundProperty(MailProperties.SENT_DATE_PROPERTY), equalTo(new MailDateFormat().parse(new MailDateFormat().format(now))));
        assertThat((String) muleMessage.getInboundProperty(customProperty), equalTo(customProperty));
    }

    @Test
    public void testAttachmentsWithSameName() throws Exception
    {
        Message payload = createMimeMessageWithSameAttachmentNames();

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage muleMessage = factory.create(payload, encoding, muleContext);
        assertThat(muleMessage.getInboundAttachmentNames(), hasSize(3));
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
