/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 */
package org.mule.transformers.compression;

import org.mule.umo.transformer.TransformerException;
import org.mule.util.compression.CompressionStrategy;
import org.mule.util.compression.GZipCompression;

/**
 * <code>GZipCompressTransformer</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class GZipUncompressTransformer extends GZipCompressTransformer
{
    private CompressionStrategy strategy;

    public GZipUncompressTransformer()
    {
        strategy = new GZipCompression();
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object src) throws TransformerException
    {
        byte[] buf = uncompressMessage(src);
        if(getReturnClass().equals(String.class)) {
            return new String(buf);
        }
        return buf;
    }

    public CompressionStrategy getStrategy()
    {
        return strategy;
    }

    public void setStrategy(CompressionStrategy strategy)
    {
        this.strategy = strategy;
    }
}
