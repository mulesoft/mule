/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;

/** TODO */
public class TransformerTemplate extends AbstractMessageTransformer
{
    private TransformerCallback callback;

    public TransformerTemplate(TransformerCallback callback)
    {
        this.callback = callback;
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding, MuleEvent event) throws TransformerMessagingException
    {
        try
        {
            return callback.doTransform(message);
        }
        catch (TransformerException e)
        {
            throw new TransformerMessagingException(e.getI18nMessage(), event, this, e);
        }
        catch (Exception e)
        {
            throw new TransformerMessagingException(event, this, e);
        }
    }

    public interface TransformerCallback
    {
        public Object doTransform(MuleMessage message) throws Exception;
    }

    public static class OverwitePayloadCallback implements TransformerCallback
    {
        private Object payload;

        public OverwitePayloadCallback(Object payload)
        {
            this.payload = payload;
        }

        public Object doTransform(MuleMessage message) throws Exception
        {
            return payload;
        }
    }
}
