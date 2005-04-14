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
package org.mule.extras.client;

import org.mule.impl.MuleMessage;
import org.mule.umo.transformer.TransformerException;

import java.util.EventObject;

/**
 * <code>EventObjectTransformer</code> converts a <code>java.util.EventObject</code>
 * into a <code>MuleMessage</code>.  This transformer is used by the MuleProxyListener
 * to marshall events into something that Mule understands.
 * @see MuleProxyListener
 * @see MuleMessage
 * @see EventObject
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class EventObjectTransformer extends AbstractEventTransformer
{
    public EventObjectTransformer()
    {
        registerSourceType(EventObject.class);
    }

    public Object doTransform(Object src) throws TransformerException
    {
        EventObject event = (EventObject)src;
        return new MuleMessage(event.getSource(), null);
    }
}
