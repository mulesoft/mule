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
package org.mule.transformers.codec;

import java.io.IOException;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.transformers.DefaultTransformer;
import org.mule.umo.transformer.TransformerException;

import sun.misc.BASE64Decoder;

/**
 * <code>Base64Encoder</code> transforms strings or byte arrays into Base64
 * encoded string
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Base64Decoder extends DefaultTransformer
{
    private BASE64Decoder decoder;

    public Base64Decoder()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(String.class);
        decoder = new BASE64Decoder();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.transformer.UMOTransformer#transform(java.lang.Object)
     */
    public Object doTransform(Object src) throws TransformerException
    {
        String data;
        if (src instanceof byte[]) {
            data = new String((byte[]) src);
        } else {
            data = (String) src;
        }
        try {
            byte[] result = decoder.decodeBuffer(data);
            if (getReturnClass().equals(String.class)) {
                return new String(result);
            }
            return result;
        } catch (IOException e) {
            throw new TransformerException(new Message(Messages.TRANSFORM_FAILED_FROM_X, "base64"), this, e);
        }
    }
}
