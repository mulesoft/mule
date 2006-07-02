/*
 * $Header: $
 * $Revision: $
 * $Date: $
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

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * Convertsa Byte array to a Hex String
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: $
 */
public class ByteArrayToHexString extends AbstractTransformer {

    public ByteArrayToHexString() {
        registerSourceType(byte[].class);
        setReturnClass(String.class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        byte[] bytes = (byte[])src;
    	if (src == null || bytes.length == 0) {
    		return "";
    	}

    	StringBuffer sb = new StringBuffer();
    	for (int i = 0; i < bytes.length; i++) {
    		int val = bytes[i] & 0x0ff;
    		if (val < 16) sb.append("0");
    		sb.append(Integer.toHexString(val));
    	}
    	return sb.toString();
    }
}
