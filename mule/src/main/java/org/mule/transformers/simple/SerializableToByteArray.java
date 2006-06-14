/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.apache.commons.lang.SerializationUtils;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

import java.io.Serializable;

/**
 * <code>SerializableToByteArray</code> converts a serializable object or a
 * String to a byte array. If <code>UMOMessage</code> is added as a source
 * type on this transformer then the UMOMessage will be serialised. This is
 * useful for transports such as tcp where the message headers would normally be
 * lost.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SerializableToByteArray extends AbstractEventAwareTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 8899970312989435192L;

    public SerializableToByteArray()
    {
        registerSourceType(Serializable.class);
        registerSourceType(byte[].class);
    }

    public Object transform(Object src, String encoding, UMOEventContext context)
            throws TransformerException
    {
        // If the UMOMessage source type has been registered that we can assume
        // that the whole message is to be serialised, not just the payload.
        // This can be useful for protocols such as tcp where the protocol does
        // not support headers, thus the whole message needs to be serialized.

        if (isSourceTypeSupported(UMOMessage.class, true)) {
            src = context.getMessage();
        }
        else {
            if (src instanceof byte[]) {
                return src;
            }
        }

        try {
            return SerializationUtils.serialize((Serializable)src);
        }
        catch (Exception e) {
            throw new TransformerException(this, e);
        }
    }
}
