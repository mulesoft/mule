/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import java.io.UnsupportedEncodingException;

import org.mule.config.i18n.Message;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>ByteArrayToString</code> converts a byte array into a String.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ByteArrayToString extends AbstractTransformer {

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
        else {
            try {
                return new String((byte[]) src, encoding);
            }
            catch (UnsupportedEncodingException e) {
                throw new TransformerException(Message.createStaticMessage("Unable to convert byte[] to String."), e);
            }
        }
    }
}
