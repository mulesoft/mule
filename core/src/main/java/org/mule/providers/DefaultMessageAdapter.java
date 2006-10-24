/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.provider.UMOMessageAdapter;

import java.util.Iterator;
import java.util.Map;

/**
 * <code>DefaultMessageAdapter</code> can be used to wrap an arbitary object where
 * no special 'apapting' is needed. The adapter allows for a set of properties to be
 * associated with an object.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class DefaultMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1908152148142575505L;

    /**
     * The message object wrapped by this adapter
     */
    protected Object message;

    /**
     * Creates a default message adapter with properties and attachments
     * 
     * @param message the message to wrap. If this is null and NullPayload object
     *            will be used
     * @see NullPayload
     */
    public DefaultMessageAdapter(Object message)
    {
        if (message == null)
        {
            this.message = new NullPayload();
        }
        else
        {
            this.message = message;
        }
    }

    public DefaultMessageAdapter(Object message, UMOMessageAdapter previous)
    {
        if (previous != null)
        {
            id = previous.getUniqueId();

            if (message == null)
            {
                this.message = new NullPayload();
            }
            else
            {
                this.message = message;
            }
            for (Iterator iterator = previous.getAttachmentNames().iterator(); iterator.hasNext();)
            {
                String name = (String)iterator.next();
                try
                {
                    addAttachment(name, previous.getAttachment(name));
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(new Message(Messages.FAILED_TO_READ_PAYLOAD), e);
                }
            }
            for (Iterator iterator = previous.getPropertyNames().iterator(); iterator.hasNext();)
            {
                String name = (String)iterator.next();
                try
                {
                    setProperty(name, previous.getProperty(name));
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(new Message(Messages.FAILED_TO_READ_PAYLOAD), e);
                }
            }
        }
        else
        {
            throw new NullPointerException("previousAdapter");
        }
    }

    /**
     * Creates a default message adapter with properties and attachments
     * 
     * @param message the message to wrap. If this is null and NullPayload object
     *            will be used
     * @param properties a map properties to set on the adapter. Can be null.
     * @param attachments a map attaches (DataHandler objects) to set on the adapter.
     *            Can be null.
     * @see NullPayload
     * @see javax.activation.DataHandler
     */
    public DefaultMessageAdapter(Object message, Map properties, Map attachments)
    {
        this(message);
        if (properties != null)
        {
            this.properties.putAll(properties);
        }
        if (attachments != null)
        {
            this.attachments.putAll(attachments);
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
        if (message instanceof byte[])
        {
            if (encoding != null)
            {
                return new String((byte[])message, encoding);
            }
            else
            {
                return new String((byte[])message);
            }
        }
        else
        {
            return message.toString();
        }
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return convertToBytes(message);
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return message;
    }

    public String getUniqueId()
    {
        return id;
    }
}
