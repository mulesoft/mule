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

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;

/**
 * <code>MailMessageAdapter</code> is a wrapper for a javax.mail.Message.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MailMessageAdapter extends AbstractMessageAdapter {

    public static final String PROPERTY_TO_ADDRESSES = "toAddresses";
    public static final String PROPERTY_FROM_ADDRESS = "fromAddress";
    public static final String PROPERTY_FROM_ADDRESSES = "fromAddresses";
    public static final String PROPERTY_CC_ADDRESSES = "ccAddresses";
    public static final String PROPERTY_BCC_ADDRESSES = "bccAddresses";
    public static final String PROPERTY_SUBJECT = "subject";

    //private Message message = null;
    private Part messagePart = null;
    private byte[] contentBuffer;

    public MailMessageAdapter(Object message) throws MessagingException {
        setMessage(message);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getPayload()
     */
    public Object getPayload() {
        return messagePart;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception {
        if (contentBuffer == null) {
            String contentType = messagePart.getContentType();

            if (contentType.startsWith("text/")) {
                getPayloadAsString();
            } else {
                InputStream is = messagePart.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024 * 32];
                int len = 0;
                while ((len = is.read(buf)) > -1) {
                    baos.write(buf, 0, len);
                }
                contentBuffer = baos.toByteArray();
            }
        }
        return contentBuffer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getPayloadAsString()
     */
    public String getPayloadAsString() throws Exception {
        if (contentBuffer == null) {
            String contentType = messagePart.getContentType();

            if (contentType.startsWith("text/")) {
                InputStream is = messagePart.getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuffer buffer = new StringBuffer();
                String line = reader.readLine();
                buffer.append(line);

                while (line != null) {
                    line = reader.readLine();
                    buffer.append(line);
                }
                contentBuffer = buffer.toString().getBytes();
            } else {
                contentBuffer = getPayloadAsBytes();
            }
        }
        return new String(contentBuffer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#setMessage(java.lang.Object)
     */
    private void setMessage(Object message) throws MessagingException {
        Message msg;
        if (message instanceof Message) {
            msg = (Message) message;
        } else {
            throw new MessageTypeNotSupportedException(message, MailMessageAdapter.class);
        }

        try {
            Object content = msg.getContent();

            if (content instanceof Multipart) {
                this.messagePart = ((Multipart) content).getBodyPart(0);
                logger.debug("Received Multipart message");
                Part part;
                String name;
                for (int i = 1; i < ((Multipart) content).getCount(); i++) {
                    part = ((Multipart) content).getBodyPart(i);
                    name = part.getDescription();
                    if (name == null) name = String.valueOf(i - 1);
                    addAttachment(name, part.getDataHandler());
                }
            } else {
                messagePart = (Part) msg;
            }

            // Set message attrributes as properties
            Address[] addresses = null;
            properties.put(PROPERTY_SUBJECT, msg.getSubject());
            addresses = msg.getFrom();
            if (addresses != null && addresses.length > 0) {
                properties.put(PROPERTY_FROM_ADDRESS, addresses[0].toString());
                properties.put(PROPERTY_FROM_ADDRESSES, addresses);
            }
            properties.put(PROPERTY_TO_ADDRESSES, msg.getRecipients(Message.RecipientType.TO));
            properties.put(PROPERTY_CC_ADDRESSES, msg.getRecipients(Message.RecipientType.CC));
            properties.put(PROPERTY_BCC_ADDRESSES, msg.getRecipients(Message.RecipientType.BCC));

            for (Enumeration e = msg.getAllHeaders(); e.hasMoreElements();) {
                Header h = (Header) e.nextElement();
                properties.put(h.getName(), h.getValue());
            }

        } catch (Exception e) {
            throw new MessagingException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X,
                    "Message Adapter"), e);
        }
    }
}
