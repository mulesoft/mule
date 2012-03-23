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

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;

import javax.activation.DataHandler;

public class CopyAttachmentsTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator attachmentNameEvaluator;

    public CopyAttachmentsTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        this.attachmentNameEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            if (!attachmentNameEvaluator.isRegularExpression())
            {
                String attachmentName = attachmentNameEvaluator.resolveStringValue(message);
                DataHandler inboundAttachment = message.getInboundAttachment(attachmentName);
                message.addOutboundAttachment(attachmentName, inboundAttachment);
            }
            else
            {
                for (String inboundAttachmentName : message.getInboundAttachmentNames())
                {
                    if (attachmentNameEvaluator.matches(inboundAttachmentName))
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
        clone.setAttachmentName(this.attachmentNameEvaluator.getRawValue());
        return clone;
    }

    public void setAttachmentName(String attachmentName)
    {
        this.attachmentNameEvaluator = new AttributeEvaluator(attachmentName).enableRegexSupport();
    }

}
