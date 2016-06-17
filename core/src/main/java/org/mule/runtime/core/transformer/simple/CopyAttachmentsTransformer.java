/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.core.api.MuleEvent;
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
        try
        {
            if (wildcardAttachmentNameEvaluator.hasWildcards())
            {
                try
                {
                    wildcardAttachmentNameEvaluator.processValues(event.getMessage().getInboundAttachmentNames(),new WildcardAttributeEvaluator.MatchCallback()
                    {
                        @Override
                        public void processMatch(String matchedValue)
                        {
                            event.setMessage(event.getMessage().transform(msg -> {
                                try
                                {
                                    msg.addOutboundAttachment(matchedValue,msg.getInboundAttachment(matchedValue));
                                }
                                catch (Exception e)
                                {
                                    throw new MuleRuntimeException(e);
                                }
                                return msg;
                            }));
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
                DataHandler inboundAttachment = event.getMessage().getInboundAttachment(attachmentName);
                event.setMessage(event.getMessage().transform(msg -> {
                    try
                    {
                        msg.addOutboundAttachment(attachmentName, inboundAttachment);
                    }
                    catch (Exception e)
                    {
                        throw new MuleRuntimeException(e);
                    }
                    return msg;
                }));
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this,e);
        }
        return event.getMessage();
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
