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

package org.mule.providers.email.transformers;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>EmailMessageToString</code> extracts a java mail Message contents and
 * returns a string.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EmailMessageToString extends AbstractTransformer
{
    public EmailMessageToString()
    {
        registerSourceType(Message.class);
        registerSourceType(String.class);
        setReturnClass(String.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src) throws TransformerException
    {
        Message msg = (Message) src;
        try {
            // Other implementations of this message should do the following
            // if they need further details of the incoming message

            // String from = msg.getFrom()[0].toString();

            // String to =
            // getAddresses(msg.getRecipients(Message.RecipientType.TO)));
            // String subject = msg.getSubject());

            // get Raw content if you want it here
            // ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // msg.writeTo(baos);
            // transMsg = new String(baos.toByteArray());
            // baos.close();

            // Hold these values on your object.
            // For this impl we just pass back the email content
            Object result = msg.getContent();
            if (result instanceof String) {
                return (String) result;
            } else {
                // very simplisitic, only gets first part
                MimeMultipart part = (MimeMultipart) result;
                String transMsg = (String) part.getBodyPart(0).getContent();
                return transMsg;
            }
        } catch (Exception e) {
            throw new TransformerException(this, e);
        }
    }

    protected String getAddresses(Address[] addr)
    {

        if (addr == null)
            return null;

        String ret = "";

        for (int i = 0; i < addr.length; i++) {
            ret += addr[i].toString() + ",";
        }
        if (ret.length() > 0)
            ret = ret.substring(0, ret.length() - 1);

        return ret;
    }

}
