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
 *
 */
package org.mule.transformers.compression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.Utility;
import org.mule.util.compression.CompressionHelper;
import org.mule.util.compression.CompressionStrategy;

import java.io.IOException;

/**
 * <code>AbstractCompressionTransformer</code> Is a base class for all transformers.
 * Transformations transform one object into another.  This base class provides facilities for
 * compressing and uncompressing messages.
 *
 * @author Ross Mason
 * @version $Revision$
 */

public abstract class AbstractCompressionTransformer extends AbstractTransformer
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(AbstractCompressionTransformer.class);
    /**
     * default constructor required for discovery
     */
    public AbstractCompressionTransformer()
    {
    }

    /**
     * @param src the source data to compress
     * @return a compressed Message as a byte[]
     * @throws TransformerException
     */
    protected byte[] compressMessage(Object src) throws TransformerException
    {
        try
        {

            byte[] buffer;
            if (src instanceof String)
            {
                buffer = ((String) src).getBytes();
            }
            else if(src instanceof byte[])
            {
                buffer = (byte[])src;
            } else {
                buffer = Utility.objectToByteArray(src);
            }
            byte[] cmp = CompressionHelper.compressByteArray(buffer);
            if (logger.isDebugEnabled())
                logger.debug("Compressed message in transformation");
            return cmp;
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }

    }

    /**
     * Uncompresses an Object into a byte[].
     *
     * @param src The Message to uncompress
     * @return
     * @throws TransformerException
     */
    protected byte[] uncompressMessage(Object src) throws TransformerException
    {
        byte[] buffer = null;
        try
        {
            if (src instanceof String)
            {
                buffer = getStrategy().uncompressByteArray(((String) src).getBytes());
            } else
            {
                buffer = getStrategy().uncompressByteArray((byte[]) src);
            }

        }
        catch (IOException e)
        {
            logger.error("Failed to uncompress message: " + e, e);
        }
        return buffer;
    }

    protected abstract CompressionStrategy getStrategy();
}