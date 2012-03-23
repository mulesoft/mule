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

import java.util.HashSet;
import java.util.Set;

public class RemoveAttachmentTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator nameEvaluator;

    public RemoveAttachmentTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        nameEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            if (!nameEvaluator.isRegularExpression())
            {
                Object keyValue = nameEvaluator.resolveValue(message);
                if (keyValue != null)
                {
                    String name = keyValue.toString();
                    message.removeOutboundAttachment(name);
                }
                else
                {
                    logger.info("Attachment key expression return null, no attachment will be removed");
                }
            }
            else
            {
                final Set<String> attachmentNames = new HashSet<String>(message.getOutboundAttachmentNames());
                for (String attachmentName : attachmentNames)
                {
                    if (nameEvaluator.matches(attachmentName))
                    {
                        message.removeOutboundAttachment(attachmentName);
                    }
                }
            }
            return message;
        }
        catch (Exception e)
        {
            throw new TransformerException(this,e);
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        RemoveAttachmentTransformer clone = (RemoveAttachmentTransformer) super.clone();
        clone.setAttachmentName(this.nameEvaluator.getRawValue());
        return clone;
    }

    public void setAttachmentName(String attachmentName)
    {
        this.nameEvaluator = new AttributeEvaluator(attachmentName).enableRegexSupport();
    }

}
