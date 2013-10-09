/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
