/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;
import org.mule.util.WildcardAttributeEvaluator;

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
    public Object transformMessage(final MuleMessage message, String outputEncoding) throws TransformerException
    {
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
                String attachmentName = attachmentNameEvaluator.resolveValue(message).toString();
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
