/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.WildcardAttributeEvaluator;

import java.nio.charset.Charset;

/**
 * TODO MULE-10179
 */
@Deprecated
public class RemovePartTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator nameEvaluator;
    private WildcardAttributeEvaluator wildcardAttributeEvaluator;

    public RemovePartTransformer()
    {
        registerSourceType(DataType.OBJECT);
        setReturnDataType(DataType.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        nameEvaluator.initialize(muleContext.getExpressionManager());
    }

    @Override
    public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException
    {
        MuleMessage message = event.getMessage();
        final Object payload = message.getPayload();

        if (!(payload instanceof MultiPartPayload))
        {
            throw new TransformerException(createStaticMessage("Cannot remove attachments/part from non-multipart payload."), this);
        }

        try
        {
            if (wildcardAttributeEvaluator.hasWildcards())
            {
                event.setMessage(MuleMessage.builder(message)
                                            .payload(((MultiPartPayload) payload).getParts()
                                                                                 .stream()
                                                                                 .filter(p -> !wildcardAttributeEvaluator.matches(((PartAttributes) p.getAttributes()).getName()))
                                                                                 .collect(toList()))
                                            .build());
            }
            else
            {
                Object keyValue = nameEvaluator.resolveValue(event);
                if (keyValue != null)
                {
                    event.setMessage(MuleMessage.builder(message)
                                                .payload(((MultiPartPayload) payload).getParts()
                                                                                     .stream()
                                                                                     .filter(p -> !((PartAttributes) p.getAttributes()).getName().equals(keyValue.toString()))
                                                                                     .collect(toList()))
                                                .build());
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
        RemovePartTransformer clone = (RemovePartTransformer) super.clone();
        clone.setAttachmentName(this.nameEvaluator.getRawValue());
        return clone;
    }

    public void setAttachmentName(String attachmentName)
    {
        this.nameEvaluator = new AttributeEvaluator(attachmentName);
        this.wildcardAttributeEvaluator = new WildcardAttributeEvaluator(attachmentName);
    }

}
