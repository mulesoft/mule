/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;

/** TODO */
public class TransformerTemplate extends AbstractMessageTransformer
{
    private TransformerCallback callback;

    public TransformerTemplate(TransformerCallback callback)
    {
        this.callback = callback;
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            return callback.doTransform(message);
        }
        catch (TransformerException e)
        {
            throw new TransformerException(e.getI18nMessage(),this, e);
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
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
