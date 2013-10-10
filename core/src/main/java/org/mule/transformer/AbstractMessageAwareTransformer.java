/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;

/**
 * <code>AbstractMessageAwareTransformer</code> is the superclass for pre-MULE 3.0 message transformers.  Newly
 * created message transformers should derive from AbstractMessageTransformer.
 *
 * @deprecated
 * @see AbstractMessageTransformer
 */
@Deprecated
public abstract class AbstractMessageAwareTransformer extends AbstractMessageTransformer
{
    /**
     * Transform the message.
     */
    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        return transform(message, outputEncoding);
    }

    /**
     * Subclasses implement this method.
     */
    public abstract Object transform(MuleMessage message, String outputEncoding) throws TransformerException;

}
