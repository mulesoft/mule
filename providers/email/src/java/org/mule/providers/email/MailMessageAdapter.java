/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.providers.email;

import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.Utility;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import java.util.Enumeration;

/**
 * <code>MailMessageAdapter</code> is a wrapper for a javax.mail.Message.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MailMessageAdapter extends AbstractMessageAdapter
{

    public static final String PROPERTY_TO_ADDRESSES = "toAddresses";
    public static final String PROPERTY_FROM_ADDRESS = "fromAddress";
    public static final String PROPERTY_FROM_ADDRESSES = "fromAddresses";
    public static final String PROPERTY_CC_ADDRESSES = "ccAddresses";
    public static final String PROPERTY_BCC_ADDRESSES = "bccAddresses";
    public static final String PROPERTY_SUBJECT = "subject";

    Message message = null;


    public MailMessageAdapter(Object message) throws MessagingException
    {
        setMessage(message);
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOMessageAdapter#getPayload()
     */
    public Object getPayload()
    {
        return message;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return Utility.objectToByteArray(message.getContent());
    }


    /* (non-Javadoc)
     * @see org.mule.providers.UMOMessageAdapter#getPayloadAsString()
     */
    public String getPayloadAsString() throws Exception
    {
        return message.getContent().toString();
    }


    /* (non-Javadoc)
     * @see org.mule.providers.UMOMessageAdapter#setMessage(java.lang.Object)
     */
    private void setMessage(Object message) throws MessagingException
    {
        if(message instanceof Message) {
            this.message = (Message)message;
        } else {
            throw new MessageTypeNotSupportedException(message, MailMessageAdapter.class);
        }
        //Set message attrributes as properties
        try
        {
            Address[] addresses = null;
            properties.put(PROPERTY_SUBJECT, this.message.getSubject());
            addresses = this.message.getFrom();
            if (addresses != null && addresses.length > 0)
            {
                properties.put(PROPERTY_FROM_ADDRESS, addresses[0].toString());
                properties.put(PROPERTY_FROM_ADDRESSES, addresses);
            }
            properties.put(PROPERTY_TO_ADDRESSES, this.message.getRecipients(Message.RecipientType.TO));
            properties.put(PROPERTY_CC_ADDRESSES, this.message.getRecipients(Message.RecipientType.CC));
            properties.put(PROPERTY_BCC_ADDRESSES, this.message.getRecipients(Message.RecipientType.BCC));

            for (Enumeration e = this.message.getAllHeaders(); e.hasMoreElements();)
            {
                Header h = (Header)e.nextElement();
                properties.put(h.getName(), h.getValue());
            }

        }
        catch (javax.mail.MessagingException e)
        {
            throw new MessagingException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X, "Message Adapter"), e);
        }
    }
}
