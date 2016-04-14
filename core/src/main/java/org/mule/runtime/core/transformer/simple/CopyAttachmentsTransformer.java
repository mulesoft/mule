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

import javax.activation.DataHandler;

public class CopyAttachmentsTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator attachmentNameEvaluator;
    private WildcardAttributeEvaluator wildcardAttachmentNameEvaluator;

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
    public Object transformMessage(MuleEvent event, String outputEncoding) throws TransformerException
    {
        MuleMessage message = event.getMessage();
        try
        {
            if (wildcardAttachmentNameEvaluator.hasWildcards())
            {
                try
                {
                    wildcardAttachmentNameEvaluator.processValues(message.getInboundAttachmentNames(),new WildcardAttributeEvaluator.MatchCallback()
                    {
                        @Override
                        public void processMatch(String matchedValue)
                        {
                            try
                            {
                                message.addOutboundAttachment(matchedValue,message.getInboundAttachment(matchedValue));
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
                String attachmentName = attachmentNameEvaluator.resolveValue(event).toString();
                DataHandler inboundAttachment = message.getInboundAttachment(attachmentName);
                message.addOutboundAttachment(attachmentName, inboundAttachment);
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
        this.attachmentNameEvaluator = new AttributeEvaluator(attachmentName);
        this.wildcardAttachmentNameEvaluator = new WildcardAttributeEvaluator(attachmentName);
    }

}
