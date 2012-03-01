/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import javax.activation.DataHandler;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;

public class CopyAttachmentsTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator keyEvaluator;

    public CopyAttachmentsTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        this.keyEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            if (keyEvaluator.isExpression() || keyEvaluator.isPlainText())
            {
                String attachmentName = keyEvaluator.resolveValue(message).toString();
                DataHandler inboundAttachment = message.getInboundAttachment(attachmentName);
                message.addOutboundAttachment(attachmentName, inboundAttachment);
            }
            else
            {
                for (String inboundAttachmentName : message.getInboundAttachmentNames())
                {
                    if (keyEvaluator.matches(inboundAttachmentName))
                    {
                        message.addOutboundAttachment(inboundAttachmentName,message.getInboundAttachment(inboundAttachmentName));
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this,e);
        }
        return message;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        CopyAttachmentsTransformer clone = (CopyAttachmentsTransformer) super.clone();
        clone.setKey(this.keyEvaluator.getRawValue());
        return clone;
    }

    public void setKey(String key)
    {
        this.keyEvaluator = new AttributeEvaluator(key).enableRegexSupport();
    }

}
