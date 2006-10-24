/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.glue;

import electric.glue.context.ThreadContext;
import electric.service.IService;

import org.apache.commons.collections.IteratorUtils;
import org.mule.config.MuleProperties;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Iterator;

/**
 * <code>GlueMessageAdapter</code> wraps a Glue soap request
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GlueMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1813666286228639967L;

    private Object message;
    private UMOTransformer trans = new SerializableToByteArray();

    public GlueMessageAdapter(Object message)
    {
        if (message instanceof GlueMessageHolder)
        {
            GlueMessageHolder holder = (GlueMessageHolder)message;
            this.message = holder.getMessage();
            Iterator iter = IteratorUtils.asIterator(holder.getService().getContext().getPropertyNames());
            String key;
            while (iter.hasNext())
            {
                key = iter.next().toString();
                setProperty(key, holder.getService().getContext().getProperty(key));
            }
        }
        else
        {
            this.message = message;
        }
        String value = (String)ThreadContext.removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if (value != null)
        {
            setReplyTo(value);
        }
        value = (String)ThreadContext.removeProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        if (value != null)
        {
            setCorrelationId(value);
        }

        value = (String)ThreadContext.removeProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        if (value != null && !"-1".equals(value))
        {
            setCorrelationSequence(Integer.parseInt(value));
        }
        value = (String)ThreadContext.removeProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        if (value != null && !"-1".equals(value))
        {
            setCorrelationGroupSize(Integer.parseInt(value));
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
        return new String(getPayloadAsBytes(), encoding);
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return (byte[])trans.transform(message);
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return message;
    }

    public static class GlueMessageHolder
    {
        private Object message;
        private IService service;

        public GlueMessageHolder(Object message, IService service)
        {
            this.message = message;
            this.service = service;
        }

        public Object getMessage()
        {
            return message;
        }

        public IService getService()
        {
            return service;
        }
    }
}
