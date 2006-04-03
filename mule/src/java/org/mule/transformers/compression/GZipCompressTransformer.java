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

import org.apache.commons.lang.SerializationUtils;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.compression.GZipCompression;

import java.io.IOException;
import java.io.Serializable;

/**
 * <code>GZipCompressTransformer</code> is a transformer compressing objects
 * into byte arrays
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GZipCompressTransformer extends AbstractCompressionTransformer
{

    public GZipCompressTransformer()
    {
        super();
        this.setStrategy(new GZipCompression());
        this.registerSourceType(Serializable.class);
        this.registerSourceType(byte[].class);
        this.setReturnClass(byte[].class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try {
            byte[] data = null;
            if (src instanceof byte[]) {
                data = (byte[])src;
            }
//            else  if (src instanceof String) {
//                data = src.toString().getBytes(encoding);
//            }
            else {
                data = SerializationUtils.serialize((Serializable)src);
            }
            return this.getStrategy().compressByteArray(data);
        }
        catch (IOException ioex) {
            throw new TransformerException(this, ioex);
        }
    }

}
