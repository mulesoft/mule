/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.transformers;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.RequestContext;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>AbstractEventAwareTransformer</code> is a transformer that has a
 * reference to the current message. This message can be used obtains properties
 * associated with the current message useful to the transform.
 * 
 * Note that when part of a transform chain, the Message payload reflects the
 * pre-transform message state, unless there is no current event for this
 * thread, then the message will be a new MuleMessage with the src as it's
 * payload. Transformers should always work on the src object not the message
 * payload.
 * 
 * @see UMOMessage
 * @see MuleMessage
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractEventAwareTransformer extends AbstractTransformer
{
    public final Object doTransform(Object src) throws TransformerException
    {
        UMOEventContext event = RequestContext.getEventContext();
        if (event == null) {
            throw new TransformerException(new Message(Messages.NO_CURRENT_EVENT_FOR_TRANSFORMER), this);
        }
        return transform(src, event);
    }

    public abstract Object transform(Object src, UMOEventContext context) throws TransformerException;
}
