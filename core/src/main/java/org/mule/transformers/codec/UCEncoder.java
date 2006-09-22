/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.codec;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>Base64Encoder</code> transforms strings or byte arrays into UU
 * encoded string
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class UCEncoder extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1120873588501386458L;

    private sun.misc.UCEncoder encoder;

    public UCEncoder()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(String.class);
        encoder = new sun.misc.UCEncoder();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.transformer.UMOTransformer#transform(java.lang.Object)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        byte[] buf;
        if (src instanceof String) {
            buf = src.toString().getBytes();
        } else {
            buf = (byte[]) src;
        }
        String result = encoder.encode(buf);
        if (getReturnClass().equals(byte[].class)) {
            return result.getBytes();
        }
        return result;
    }
}
