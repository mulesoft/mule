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
import org.mule.util.compression.CompressionStrategy;
import org.mule.util.compression.GZipCompression;

/**
 * <code>GZipCompressTransformer</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GZipCompressTransformer extends AbstractCompressionTransformer
{
    private CompressionStrategy strategy;

    public GZipCompressTransformer()
    {
        strategy = new GZipCompression();
        registerSourceType(Object.class);
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object src) throws TransformerException
    {
        return compressMessage(src);
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
