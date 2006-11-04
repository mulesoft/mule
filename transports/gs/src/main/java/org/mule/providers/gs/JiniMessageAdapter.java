/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs;

import net.jini.core.entry.Entry;

import org.apache.commons.lang.StringUtils;
import org.mule.MuleManager;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.UUID;

/**
 * <code>JiniMessageAdapter</code> wraps a JavaSpaceMessage entry.
 * 
 * @see Entry
 */

public class JiniMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3480872474322069206L;

    protected Entry message;
    protected String id;

    public JiniMessageAdapter(Object message) throws MessagingException
    {
        if (message == null)
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }

        if (message instanceof Entry)
        {
            this.message = (Entry)message;
        }
        else
        {
            this.message = new JiniMessage(null, message);
        }

        if (this.message instanceof JiniMessage)
        {
            JiniMessage jm = (JiniMessage)this.message;

            // accept or create
            this.id = jm.getMessageId();
            if (this.id == null)
            {
                id = UUID.getUUID();
            }

            // accept null
            this.setCorrelationId(jm.getCorrelationId());

            // accept or default
            Integer value = jm.getCorrelationGroupSize();
            this.setCorrelationGroupSize(value != null ? value.intValue() : -1);

            // accept or default
            value = jm.getCorrelationSequence();
            this.setCorrelationSequence(value != null ? value.intValue() : -1);

            // accept null
            this.setReplyTo(jm.getReplyTo());

            // accept or default
            this.setEncoding(StringUtils.defaultIfEmpty(jm.getEncoding(), MuleManager.getConfiguration()
                .getEncoding()));

            // accept null
            this.setExceptionPayload(jm.getExceptionPayload());

            // accept all (as handled by superclass)
            this.addProperties(jm.getProperties());
        }
        else
        {
            id = UUID.getUUID();
        }
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        if (message instanceof JiniMessage)
        {
            Object payload = ((JiniMessage)message).getPayload();
            if (payload != null)
            {
                return payload.toString();
            }
            else
            {
                return null;
            }
        }
        else
        {
            return message.toString();
        }
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        Object payload = message;
        if (message instanceof JiniMessage)
        {
            payload = ((JiniMessage)message).getPayload();
        }

        return convertToBytes(payload);
    }

    public Object getPayload()
    {
        if (message instanceof JiniMessage)
        {
            return ((JiniMessage)message).getPayload();
        }
        else
        {
            return message;
        }
    }

    public String getUniqueId()
    {
        return id;
    }
}
