/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.providers.email.transformers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.email.MailConnector;
import org.mule.providers.email.Pop3Connector;
import org.mule.providers.email.SmtpConnector;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.transformer.TransformerException;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Calendar;


/**
 * <code>StringToEmailMessage</code> will convert a string to a java mail Message, using the
 * string as the contents.  This implementation uses properties on the transformer
 * to determine the to and subject fields.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class StringToEmailMessage extends AbstractEventAwareTransformer
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(StringToEmailMessage.class);

    protected InternetAddress[] inetToAddresses = null;
    protected InternetAddress inetFromAddress;
    protected String subject;
    protected String toAddresses;
    protected String fromAddress;

    public StringToEmailMessage()
    {
        registerSourceType(String.class);
    }

    /* (non-Javadoc)
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object transform(Object src, UMOEventContext context) throws TransformerException
    {
        String contentType = (String)context.getProperty("contentType");
        if(contentType==null) contentType = "text/plain";

        try
        {
            Message msg = new MimeMessage((Session) endpoint.getConnector().getDispatcher("ANY").getDelegateSession());

            msg.setRecipients(Message.RecipientType.TO, inetToAddresses);

            //sent date
            msg.setSentDate(Calendar.getInstance().getTime());

            if (inetFromAddress != null)
            {
                msg.setFrom(inetFromAddress);
            }

            msg.setSubject(getSubject());

            //attachments TODO
            msg.setContent(src, contentType);
            return msg;
        }
        catch (Exception e)
        {
            throw new TransformerException("Failed to create email message: " + e, e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.transformer.UMOTransformer#setConnector(org.mule.umo.provider.Connector)
     */
    public void setEndpoint(UMOImmutableEndpoint endpoint)
    {
        if (endpoint == null)
        {
            super.setEndpoint(endpoint);
        } else if( endpoint.getConnector() instanceof MailConnector || endpoint.getConnector() instanceof SmtpConnector
         || endpoint.getConnector() instanceof Pop3Connector)
        {
            super.setEndpoint(endpoint);
        }
        else
        {
            throw new IllegalArgumentException("The endpoint connector for an Email transfromer must be of type: " + MailConnector.class.getName() +
                    " or " + SmtpConnector.class.getName() +
                    " or " + Pop3Connector.class.getName() +
                    " not " + (endpoint.getConnector() == null ? "null" : endpoint.getConnector().getClass().getName()));
        }
    }

    /**
     * @return Returns the fromAddress.
     */
    public String getFromAddress()
    {
        return fromAddress;
    }

    /**
     * @param fromAddress The fromAddress to set.
     */
    public void setFromAddress(String fromAddress) throws TransformerException
    {
        if (!(fromAddress == null || "".equals(fromAddress)))
        {
            try
            {
                inetFromAddress = new InternetAddress(fromAddress);
            }
            catch (AddressException e)
            {
                throw new TransformerException("Failed to parse the from address: " + fromAddress + ". " + e.getMessage(), e);
            }
        }
        this.fromAddress = fromAddress;
    }

    /**
     * @return Returns the subject.
     */
    public String getSubject()
    {
        if (subject == null) subject = "";
        return subject;
    }

    /**
     * @param subject The subject to set.
     */
    public void setSubject(String subject)
    {
        this.subject = subject;
        if (subject == null)
        {
            subject = "[No Subject]";
            logger.warn("Emails addressed to: " + toAddresses + " will be sent with a subject [no subject]");
        }
    }

    /**
     * @return Returns the toAddress.
     */
    public String getToAddress()
    {
        return toAddresses;
    }

    /**
     * @param toAddress The toAddress to set.
     */
    public void setToAddress(String toAddress) throws TransformerException
    {
        if (!(toAddress == null || "".equals(toAddress)))
        {
            try
            {
                inetToAddresses = InternetAddress.parse(toAddress, false);
            }
            catch (AddressException e)
            {
                throw new TransformerException("Failed to parse the to addresses: " + toAddress + ". " + e.getMessage(), e);
            }
        }
        this.toAddresses = toAddress;
    }

}
