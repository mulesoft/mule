/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ThreadSafeAccess;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMutableMessageAdapter;

import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * <code>DefaultMessageAdapter</code> can be used to wrap an arbitary object where
 * no special 'apapting' is needed. The adapter allows for a set of properties to be
 * associated with an object.
 */

public final class DefaultMessageAdapter extends AbstractMessageAdapter implements UMOMutableMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1908152148142575505L;

    /**
     * The message object wrapped by this adapter
     */
    protected Object message;

    
    protected DefaultMessageAdapter()
    {
        super();
    }

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
            this.message = NullPayload.getInstance();
        }
        else
        {
            this.message = message;
        }
    }

    public DefaultMessageAdapter(Object message, UMOMessageAdapter previous)
    {
        super(previous);
        if (previous != null)
        {
            if (message == null)
            {
                this.message = NullPayload.getInstance();
            }
            else
            {
                this.message = message;
            }
            // MULE-1564
            // this is an iterator over a concurrent map and so is weakly consistent (not fail-safe)
            // that means we don't get errors here, but may see changed values.
            // so we can make this safe to null values (although not predictable) by simply checking values
            for (Iterator iterator = previous.getAttachmentNames().iterator(); iterator.hasNext();)
            {
                String name = (String) iterator.next();
                try
                {
                    DataHandler dh = previous.getAttachment(name);
                    if (null == dh)
                    {
                        logger.warn("Detected concurrent access to attachment " + name + " for " + previous);
//                        new Throwable().printStackTrace();
                    }
                    else
                    {
                        addAttachment(name, dh);
                    }
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToReadPayload(), e);
                }
            }
            for (Iterator iterator = previous.getPropertyNames().iterator(); iterator.hasNext();)
            {
                String name = (String) iterator.next();
                try
                {
                    Object value = previous.getProperty(name);
                    if (null == value)
                    {
                        logger.warn("Detected concurrent access to property " + name + " for " + previous);
//                        new Throwable().printStackTrace();
                    }
                    else
                    {
                        setProperty(name, value);
                    }
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToReadPayload(), e);
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("previousAdapter may not be null");
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
            this.properties.addInboundProperties(properties);
        }
        if (attachments != null)
        {
            this.attachments.putAll(attachments);
        }
    }

    /** {@inheritDoc} */
    public Object getPayload()
    {
        return message;
    }

    /** {@inheritDoc} */
    public void setPayload(Object payload)
    {
        synchronized(message)
        {
            message = payload;
        }
    }

    /** {@inheritDoc} */
    public String getUniqueId()
    {
        return id;
    }

    ////////////////////////// ThreadSafeAccess impl ////////////////////

    /** {@inheritDoc} */
    public ThreadSafeAccess newThreadCopy()
    {
        return new DefaultMessageAdapter(getPayload(), this);
    }

}
