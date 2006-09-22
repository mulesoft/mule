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
 * <code>StringToByteArray</code> converts a string into a byte array
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class StringToByteArray extends AbstractTransformer {

    private static final long serialVersionUID = 3993746463869846673L;

    public StringToByteArray()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof byte[]) {
            return src;
        }
        else {
            try {
                return ((String) src).getBytes(encoding);
            }
            catch (UnsupportedEncodingException e) {
                throw new TransformerException(Message.createStaticMessage("Unable to convert String to byte[]."), e);
            }
        }
    }
}
