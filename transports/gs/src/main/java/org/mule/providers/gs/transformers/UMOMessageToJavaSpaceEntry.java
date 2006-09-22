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

import net.jini.core.entry.Entry;
import org.mule.providers.gs.JiniMessage;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Convers an outbound event ot a JavaSpace entry that can be written to the space.
 *
 * @see net.jini.core.entry.Entry
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UMOMessageToJavaSpaceEntry extends AbstractEventAwareTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6852740125237850362L;

    public UMOMessageToJavaSpaceEntry() {
        setReturnClass(Entry.class);
    }

    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException {
        if(src instanceof Entry) {
            return src;
        } else {
            String destination = context.getEndpointURI().toString();
            JiniMessage msg = new JiniMessage(destination, src);
            msg.setMessageId(context.getMessage().getUniqueId());
            msg.setCorrelationId(context.getMessage().getCorrelationId());
            msg.setCorrelationGroupSize(new Integer(context.getMessage().getCorrelationGroupSize()));
            msg.setCorrelationSequence(new Integer(context.getMessage().getCorrelationSequence()));
            msg.setReplyTo(context.getMessage().getReplyTo());
            msg.setEncoding(context.getMessage().getEncoding());
            msg.setExceptionPayload(context.getMessage().getExceptionPayload());
            Map props = new HashMap();
            for (Iterator iterator = context.getMessage().getPropertyNames().iterator(); iterator.hasNext();) {
                String key = (String)iterator.next();
                props.put(key, context.getMessage().getProperty(key));
            }
            msg.setProperties(props);
            return msg;
        }
    }
}
