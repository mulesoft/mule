/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs.transformers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.jini.core.entry.Entry;

import org.mule.providers.gs.JiniMessage;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

/**
 * Convers an outbound event ot a JavaSpace entry that can be written to the space.
 * 
 * @see net.jini.core.entry.Entry
 */
public class UMOMessageToJavaSpaceEntry extends AbstractEventAwareTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6852740125237850362L;

    public UMOMessageToJavaSpaceEntry()
    {
        setReturnClass(Entry.class);
    }

    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        if (src instanceof Entry)
        {
            return src;
        }
        else
        {
            String destination = context.getEndpointURI().toString();
            UMOMessage muleMessage = context.getMessage();

            JiniMessage msg = new JiniMessage(destination, src);
            msg.setMessageId(muleMessage.getUniqueId());
            msg.setCorrelationId(muleMessage.getCorrelationId());
            msg.setCorrelationGroupSize(new Integer(muleMessage.getCorrelationGroupSize()));
            msg.setCorrelationSequence(new Integer(muleMessage.getCorrelationSequence()));
            msg.setReplyTo(muleMessage.getReplyTo());
            msg.setEncoding(muleMessage.getEncoding());
            msg.setExceptionPayload(muleMessage.getExceptionPayload());

            Map props = new HashMap();
            for (Iterator iterator = muleMessage.getPropertyNames().iterator(); iterator.hasNext();)
            {
                String key = (String)iterator.next();
                props.put(key, muleMessage.getProperty(key));
            }

            msg.setProperties(props);
            return msg;
        }
    }

}
