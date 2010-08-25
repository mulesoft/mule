/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

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
    
    public void testAttachments() throws Exception
    {
        Message payload = createMimeMessageWithAttachment();

        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage muleMessage = factory.create(payload, encoding);
        // TODO MULE-5034 wrong deprecation, replacing with getInboundAttachmentNames() breaks the test
        assertEquals(2, muleMessage.getAttachmentNames().size());
    }
    
    private Message createMimeMessageWithAttachment() throws Exception
    {
        MimeBodyPart mainBody = new MimeBodyPart();
        mainBody.setText("This is the main message text");
        
        MimeBodyPart attachment = new MimeBodyPart();
        DataSource source = new ByteArrayDataSource(TEST_MESSAGE.getBytes(), "text/plain");
        attachment.setDataHandler(new DataHandler(source));
        attachment.setFileName("message.txt");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mainBody);
        multipart.addBodyPart(attachment);
          
        MimeMessage message = getValidTransportMessage();
        message.setContent(multipart);
        return message;
    }
}
