/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.client;

import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.TransformerException;

import java.util.EventObject;

/**
 * <code>EventObjectTransformer</code> converts a
 * <code>java.util.EventObject</code> into a <code>DefaultMuleMessage</code>. This
 * transformer is used by the MuleProxyListener to marshall events into something
 * that Mule understands.
 * 
 * @see MuleProxyListener
 * @see DefaultMuleMessage
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
        return new DefaultMuleMessage(((EventObject)src).getSource());
    }

}
