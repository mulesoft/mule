/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    public Object transformMessage(final MuleMessage message, String outputEncoding) throws TransformerException
    {
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
