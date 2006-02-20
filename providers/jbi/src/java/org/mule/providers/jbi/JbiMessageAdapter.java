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
 */
package org.mule.providers.jbi;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

import javax.activation.DataHandler;
import javax.jbi.messaging.NormalizedMessage;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>JbiMessageAdapter</code> translates a JBI NormalizedMessage
 * to a UMOMessage
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class JbiMessageAdapter extends AbstractMessageAdapter
{
    private NormalizedMessage message;

    public JbiMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof NormalizedMessage) {
            this.message = (NormalizedMessage) message;
            for (Iterator iterator = this.message.getPropertyNames().iterator(); iterator.hasNext();) {
                String s = (String) iterator.next();
                properties.put(s, this.message.getProperty(s));
            }
        } else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }


    public void setProperty(Object key, Object value)
    {
        message.setProperty(key.toString(), value);
    }


    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if necessary). The parameter is
     *                 used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception {
        throw new UnsupportedOperationException("getPayloadAsString");
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception {
        throw new UnsupportedOperationException("getPayloadAsBytes");
    }

    public Object getPayload()
    {
        return message;
    }

    public Object getProperty(Object key) {
        return message.getProperty(key.toString());
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception {
        message.addAttachment(name, dataHandler);
    }

    public void removeAttachment(String name) throws Exception {
        message.removeAttachment(name);
    }

    public DataHandler getAttachment(String name) {
        return message.getAttachment(name);
    }

    public Set getAttachmentNames(){
        return message.getAttachmentNames();
    }

}
