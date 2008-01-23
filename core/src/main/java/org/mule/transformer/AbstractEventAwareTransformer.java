/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer;

import org.mule.RequestContext;
import org.mule.api.MuleEventContext;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;

/**
 * <code>AbstractEventAwareTransformer</code> is a transformer that has a reference
 * to the current message. This message can be used obtains properties associated
 * with the current message useful to the transform. Note that when part of a
 * transform chain, the Message payload reflects the pre-transform message state,
 * unless there is no current event for this thread, then the message will be a new
 * DefaultMuleMessage with the src as its payload. Transformers should always work on the
 * src object not the message payload.
 * 
 * @see org.mule.api.MuleMessage
 * @see org.mule.DefaultMuleMessage
 * @see org.mule.transformer.AbstractMessageAwareTransformer
 * @deprecated use AbstractMessageAwareTransformer
 *
 */

public abstract class AbstractEventAwareTransformer extends AbstractTransformer
{
    public final Object doTransform(Object src, String encoding) throws TransformerException
    {
        MuleEventContext event = RequestContext.getEventContext();
        if (event == null && requiresCurrentEvent())
        {
            throw new TransformerException(CoreMessages.noCurrentEventForTransformer(), this);
        }
        return transform(src, encoding, event);
    }

    public abstract Object transform(Object src, String encoding, MuleEventContext context)
        throws TransformerException;

    protected boolean requiresCurrentEvent()
    {
        return true;
    }
}
