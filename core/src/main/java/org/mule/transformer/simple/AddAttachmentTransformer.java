/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

import java.text.MessageFormat;

public class AddAttachmentTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator nameEvaluator;
    private AttributeEvaluator valueEvaluator;
    private AttributeEvaluator contentTypeEvaluator;

    public AddAttachmentTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        nameEvaluator.initialize(muleContext.getExpressionManager());
        valueEvaluator.initialize(muleContext.getExpressionManager());
        contentTypeEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            Object keyValue = nameEvaluator.resolveValue(message);
            if (keyValue == null)
            {
                logger.error("Setting Null attachment key is not supported, this entry is being ignored");
            }
            else
            {
                String key = keyValue.toString();
                Object value = valueEvaluator.resolveValue(message);
                if (value == null)
                {
                    logger.error(MessageFormat.format(
                         "Attachment with key ''{0}'', not found on message using ''{1}''. Since the value was marked optional, nothing was set on the message for this attachment",
                         key, valueEvaluator.getRawValue()));
                }
                else
                {
                    String contentType = contentTypeEvaluator.resolveValue(message).toString();
                    message.addOutboundAttachment(key,value,contentType);
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
        AddAttachmentTransformer clone = (AddAttachmentTransformer) super.clone();
        clone.setName(this.nameEvaluator.getRawValue());
        clone.setValue(this.valueEvaluator.getRawValue());
        return clone;
    }

    public void setAttachmentName(String attachmentName)
    {
        this.nameEvaluator = new AttributeEvaluator(attachmentName);
    }

    public void setValue(String value)
    {
        this.valueEvaluator = new AttributeEvaluator(value);
    }

    public void setContentType(String contentType)
    {
        this.contentTypeEvaluator = new AttributeEvaluator(contentType);
    }
}
