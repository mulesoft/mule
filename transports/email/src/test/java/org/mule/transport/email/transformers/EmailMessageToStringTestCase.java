
package org.mule.transport.email.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.email.GreenMailUtilities;

import java.io.StringBufferInputStream;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailMessageToStringTestCase extends AbstractMuleTestCase
{

    private EmailMessageToString transformer;
    private static String TEXT = "text";
    private static String TO = "me@me.com";

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        transformer = new EmailMessageToString();
    }

    public void testSimpleStringMessage() throws MessagingException, TransformerException
    {
        Message msg = GreenMailUtilities.toMessage(TEXT, TO, null);
        assertEquals(TEXT, transformer.transform(msg));
    }

    public void testSimpleNonTextMessage() throws MessagingException, TransformerException
    {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setContent(new StringBufferInputStream(TEXT), "application/octet-stream");
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        assertEquals("", transformer.transform(message));
    }

    public void testMultipartFirstPartTextMessage() throws MessagingException, TransformerException
    {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        MimeMultipart mimeMultipart = new MimeMultipart();
        MimeBodyPart bp1 = new MimeBodyPart();
        bp1.setContent(TEXT, "test/plain");
        mimeMultipart.addBodyPart(bp1);
        message.setContent(mimeMultipart);
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        assertEquals(TEXT, transformer.transform(message));
    }

    public void testMultipartMessage2() throws MessagingException, TransformerException
    {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        MimeMultipart mimeMultipart = new MimeMultipart();
        MimeBodyPart bp1 = new MimeBodyPart();
        bp1.setContent(new StringBufferInputStream(TEXT), "binary");
        mimeMultipart.addBodyPart(bp1);
        message.setContent(mimeMultipart);
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        assertEquals("", transformer.transform(message));
    }

}
