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

import java.util.HashSet;
import java.util.Set;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;

public class RemoveAttachmentTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator keyEvaluator;

    public RemoveAttachmentTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        keyEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            if (keyEvaluator.isExpression() || keyEvaluator.isPlainText())
            {
                Object keyValue = keyEvaluator.resolveValue(message);
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
                    if (keyEvaluator.matches(attachmentName))
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
        clone.setKey(this.keyEvaluator.getRawValue());
        return clone;
    }

    public void setKey(String key)
    {
        this.keyEvaluator = new AttributeEvaluator(key).enableRegexSupport();
    }

}
