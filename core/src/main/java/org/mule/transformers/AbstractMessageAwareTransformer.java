/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers;

import org.mule.util.DebugOptions;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>AbstractMessageAwareTransformer</code> is a transformer that has a reference
 * to the current message. This message can be used obtains properties associated
 * with the current message useful to the transform. Note that when part of a
 * transform chain, the Message payload reflects the pre-transform message state,
 * unless there is no current event for this thread, then the message will be a new
 * MuleMessage with the src as its payload. Transformers should always work on the
 * src object not the message payload.
 *
 * @see org.mule.umo.UMOMessage
 * @see org.mule.impl.MuleMessage
 */

public abstract class AbstractMessageAwareTransformer extends AbstractTransformer
{

    public boolean isSourceTypeSupported(Class aClass, boolean exactMatch)
    {
        //TODO RM* This is a bit of hack since we could just register UMOMessage as a supportedType, but this has some
        //funny behaviour in certain ObjectToXml transformers
        return (super.isSourceTypeSupported(aClass, exactMatch) || UMOMessage.class.isAssignableFrom(aClass)); 
    }

    public final Object doTransform(Object src, String encoding) throws TransformerException
    {
        UMOMessage message;
        if(src instanceof UMOMessage)
        {
            message = (UMOMessage)src;
        }
        else if(DebugOptions.isAutoWrapMessageAwareTransform())
        {
            message = new MuleMessage(src);
        }
        else
        {
            UMOEventContext event = RequestContext.getEventContext();
            if (event == null)
            {
                throw new TransformerException(CoreMessages.noCurrentEventForTransformer(), this);
            }
            message = event.getMessage();
            if(!message.getPayload().equals(src))
            {
                throw new IllegalStateException("Transform payload does not match current EventContext payload");
            }
        }
        return transform(message, encoding);
    }

    public abstract Object transform(UMOMessage message, String outputEncoding)
        throws TransformerException;

}