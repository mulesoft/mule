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
 *
 */
package org.mule.transformers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.Utility;
import org.mule.util.compression.CompressionHelper;

import java.io.IOException;

/**
 * <code>CompressionTransformer</code> Is a base class for all transformers.
 * Transformations transform one object into another.  This base class provides facilities for
 * compressing and uncompressing messages.
 *
 * @author Ross Mason
 * @version $Revision$
 */

public abstract class CompressionTransformer extends AbstractTransformer
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(CompressionTransformer.class);

    private boolean doCompression = false;

    /**
     * default constructor required for discovery
     */
    public CompressionTransformer()
    {
    }

    /**
     * Transforms the object.
     *
     * @param src           The source object to transform.
     * @param doCompression determines weather the data should be compressed or not
     * @return The transformed object
     */
    public Object transform(Object src, boolean doCompression) throws TransformerException, TransformerException
    {
        setDoCompression(doCompression);
        return transform(src);
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
            if (getDoCompression())
            {
                byte[] buffer;
                if (src instanceof String)
                {
                    buffer = ((String) src).getBytes();
                }
                else
                {
                    buffer = Utility.objectToByteArray(src);
                }
                byte[] cmp = CompressionHelper.compressByteArray(buffer);
                if (logger.isDebugEnabled())
                    logger.debug("Compressed message in transformation");
                return cmp;

            }
            else
            {
                return Utility.objectToByteArray(src);
            }
        }
        catch (IOException e)
        {
            throw new TransformerException("Failed to compress message", e);
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
                buffer = CompressionHelper.uncompressByteArray(((String) src).getBytes());
            }
            else
            {
                buffer = CompressionHelper.uncompressByteArray(Utility.objectToByteArray(src));
            }
        }
        catch (IOException e)
        {
            logger.error("Failed to uncompress message: " + e, e);
        }
        return buffer;
    }

    /**
     * @return True if compression will be uesed otherwise false
     */
    public boolean getDoCompression()
    {
        return doCompression;
    }

    /**
     * @param doCompression determines whether compression is used
     */
    public void setDoCompression(boolean doCompression)
    {
        this.doCompression = doCompression;
    }
}