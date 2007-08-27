/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.client;

import org.mule.impl.MuleMessage;
import org.mule.umo.transformer.TransformerException;

import java.util.EventObject;

/**
 * <code>EventObjectTransformer</code> converts a
 * <code>java.util.EventObject</code> into a <code>MuleMessage</code>. This
 * transformer is used by the MuleProxyListener to marshall events into something
 * that Mule understands.
 * 
 * @see MuleProxyListener
 * @see MuleMessage
 * @see EventObject
 */

public class EventObjectTransformer extends AbstractEventTransformer
{

    public EventObjectTransformer()
    {
        registerSourceType(EventObject.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        return new MuleMessage(((EventObject)src).getSource());
    }

}
