/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.transformers;

import org.mule.umo.transformer.TransformerException;

import javax.jms.Message;

/**
 * <code>JMSMessageToObject</code> Will convert a <code>javax.jms.Message</code>
 * or sub-type into an object by extracting the message payload. Users of this
 * transformer can set different return types on the transform to control the way it
 * behaves.
 * <ul>
 * <li>javax.jms.TextMessage - java.lang.String</li>
 * <li>javax.jms.ObjectMessage - java.lang.Object</li>
 * <li>javax.jms.BytesMessage - Byte[]. Note that the transformer will check if the
 * payload is compressed and automatically uncompress the message.</li>
 * <li>javax.jms.MapMessage - java.util.Map</li>
 * <li>javax.jms.StreamMessage - java.util.Vector of objects from the Stream
 * Message.</li>
 * </ul>
 */

public class JMSMessageToObject extends AbstractJmsTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4458860619942940372L;

    public JMSMessageToObject()
    {
        super();
        registerSourceType(Message.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Source object is " + src.getClass().getName());
            }

            Object result = transformFromMessage((Message)src);

            if (logger.isDebugEnabled())
            {
                logger.debug("Resulting object is " + result.getClass().getName());
            }

            return result;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
