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

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.StringMessageUtils;

/**
 * <code>ByteArrayToString</code> converts a byte array into a String.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ByteArrayToString extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -9033005899991305308L;

    public ByteArrayToString()
    {
        registerSourceType(byte[].class);
        registerSourceType(String.class);
        setReturnClass(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof String) {
            return src;
        }
        if (encoding != null) {
            return StringMessageUtils.getString((byte[])src, encoding);
        } else {
            return StringMessageUtils.getString((byte[])src);
        }
    }
}
