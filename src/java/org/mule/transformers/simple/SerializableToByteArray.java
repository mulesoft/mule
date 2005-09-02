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
package org.mule.transformers.simple;

import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * <code>SerializableToByteArray</code> converts a serializable object or a
 * String to a byte array. If <code>UMOMessage</code> is added as a source type on
 * this transformer then the UMOMessage will be serialised.  This is useful for transports
 * such as tcp where the message headers would normally be lost.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SerializableToByteArray extends AbstractEventAwareTransformer
{
    public SerializableToByteArray()
    {
        registerSourceType(Serializable.class);
        registerSourceType(String.class);
        registerSourceType(byte[].class);
    }

    public Object transform(Object src, UMOEventContext context) throws TransformerException {

        Object obj = null;
        //If the UMOMessage source type has been registered that we can assume that the
        //whole message is to be serialised to Xml, nit just the payload.  This can be useful
        //for protocols such as tcp where the protocol does not support headers, thus the whole messgae
        //needs to be serialized
        if(isSourceTypeSupported(UMOMessage.class, true)) {
            obj = context.getMessage();
        } else {
            obj = src;
            if (obj instanceof byte[]) {
                return (byte[]) obj;
            }
        }

        byte[] dest = null;
        ByteArrayOutputStream bs = null;
        ObjectOutputStream os = null;
        try {
            bs = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bs);
            os.writeObject(obj);
            os.flush();
            dest = bs.toByteArray();
        } catch (Exception e) {
            throw new TransformerException(this, e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return dest;
    }
}
