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

package org.mule.transformers.compression;

import org.mule.umo.transformer.TransformerException;
import org.mule.util.compression.GZipCompression;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * <code>GZipCompressTransformer</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GZipUncompressTransformer extends GZipCompressTransformer
{

    public GZipUncompressTransformer()
    {
        super();
        this.setStrategy(new GZipCompression());
        this.registerSourceType(byte[].class);
        this.setReturnClass(byte[].class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        byte[] buffer = null;

        try {
            buffer = getStrategy().uncompressByteArray((byte[])src);
        }
        catch (IOException e) {
            logger.error("Failed to uncompress message:", e);
            throw new TransformerException(this, e);
        }

        if (getReturnClass().equals(String.class)) {
            if (encoding != null) {
                try {
                    return new String(buffer, encoding);
                }
                catch (UnsupportedEncodingException ex) {
                    return new String(buffer);
                }
            }
            else {
                return new String(buffer);
            }
        }

        return buffer;
    }

}
