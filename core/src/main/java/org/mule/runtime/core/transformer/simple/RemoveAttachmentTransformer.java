/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.WildcardAttributeEvaluator;

public class RemoveAttachmentTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator nameEvaluator;
    private WildcardAttributeEvaluator wildcardAttributeEvaluator;

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
    public Object transformMessage(MuleEvent event, String outputEncoding) throws TransformerException
    {
        MuleMessage message = event.getMessage();
        try
        {
            if (wildcardAttributeEvaluator.hasWildcards())
            {
                try
                {
                    wildcardAttributeEvaluator.processValues(message.getOutboundAttachmentNames(),new WildcardAttributeEvaluator.MatchCallback()
                    {
                        @Override
                        public void processMatch(String matchedValue)
                        {
                            try
                            {
                                message.removeOutboundAttachment(matchedValue);
                            } catch (Exception e)
                            {
                                throw new MuleRuntimeException(e);
                            }
                        }
                    });
                }
                catch (Exception e)
                {
                    throw new TransformerException(this,e);
                }
            }
            else
            {
                Object keyValue = nameEvaluator.resolveValue(event);
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
        this.nameEvaluator = new AttributeEvaluator(attachmentName);
        this.wildcardAttributeEvaluator = new WildcardAttributeEvaluator(attachmentName);
    }

}
