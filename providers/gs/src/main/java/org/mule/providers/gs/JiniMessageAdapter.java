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
package org.mule.providers.gs;

import net.jini.core.entry.Entry;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.UUID;

import java.util.Iterator;

/**
 * <code>JiniMessageAdapter</code> wraps a JavaSpaceMessage entry.
 *
 * @see Entry
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class JiniMessageAdapter extends AbstractMessageAdapter
{
    protected Entry message;
    protected String id;

    public JiniMessageAdapter(Object message) throws MessagingException
    {
        if(message==null) {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
        if(message instanceof Entry) {
            this.message = (Entry)message;
        } else {
            this.message = new JiniMessage(null, message);
        }

        if(this.message instanceof JiniMessage) {
            JiniMessage jm = (JiniMessage)this.message;
            id = (jm.getMessageId());
            if(id==null) id = UUID.getUUID();

            setCorrelationId(jm.getCorrelationId());
            setCorrelationGroupSize(jm.getCorrelationGroupSize().intValue());
            setCorrelationSequence(jm.getCorrelationSequence().intValue());
            setReplyTo(jm.getReplyTo());
            setEncoding(jm.getEncoding());
            setExceptionPayload(jm.getExceptionPayload());
            for (Iterator iterator = jm.getProperties().keySet().iterator(); iterator.hasNext();) {
                Object o = iterator.next();
                setProperty(o, jm.getProperties().get(o));
            }
        } else {
            id = UUID.getUUID();
        }
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
        if(message instanceof JiniMessage) {
            return ((JiniMessage)message).getPayload().toString();
        } else {
            return message.toString();
        }
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        Object payload = message;
        if(message instanceof JiniMessage) {
            payload = ((JiniMessage)message).getPayload();
        }

        return convertToBytes(payload);
    }

    public Object getPayload()
    {
        if(message instanceof JiniMessage) {
            return ((JiniMessage)message).getPayload();
        } else {
            return message;
        }
    }

    public String getUniqueId() {
        return id;
    }
}
