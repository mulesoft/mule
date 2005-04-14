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

package org.mule.util.compression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.ClassHelper;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * <code>CompressionHelper</code> a static class that provides facilities for compressing and uncompressing
 * byte arrays
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class CompressionHelper
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(CompressionHelper.class);

    private static CompressionStrategy strategy = getCompressionStrategy();

    public static final CompressionStrategy getCompressionStrategy()
    {
        return (CompressionStrategy) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                try
                {
                    //Object o = DiscoverSingleton.find(CompressionStrategy.class, CompressionStrategy.COMPRESSION_DEFAULT);
                    Object o = ClassHelper.loadClass(CompressionStrategy.COMPRESSION_DEFAULT, CompressionHelper.class).newInstance();
                    logger.debug("Found CompressionStrategy: " + o.getClass().getName());
                    return o;
                } catch (Exception e)
                {
                    logger.warn("Failed to build compression strategy: " + e.getMessage());
                }
                return null;
            }
        });
    }

    /**
     * Used for compressing  a byte array into a new byte array using the
     * CompressionStategy found using discovery
     *
     * @param bytes An array of bytes to compress
     * @return a compressed byte array
     * @throws java.io.IOException if it fails to write to a GZIPOutputStream
     * @see java.util.zip.GZIPOutputStream
     */
    public static byte[] compressByteArray(byte[] bytes) throws IOException
    {
        logger.debug("Compressing message of size: " + bytes.length);
        byte[] compressedByteArray = strategy.compressByteArray(bytes);
        logger.debug("Compressed message to size: " + compressedByteArray.length);
        return compressedByteArray;
    }

    /**
     * Used for uncompressing a byte array into a uncompressed byte array using the
     * CompressionStategy found using discovery
     *
     * @param bytes An array of bytes to uncompress
     * @return an uncompressed byte array
     * @throws java.io.IOException if it fails to read from a InputStream
     */
    public static byte[] uncompressByteArray(byte[] bytes) throws IOException
    {
        logger.debug("Uncompressing message of size: " + bytes.length);
        if (!strategy.isCompressed(bytes))
        {
            //throw a specific exception here to allow users of this method to deffientiate between
            //general IOExceptions and an Invalid format
            logger.warn("data is not of type GZIP compressed. The data may not have been compressed in the first place");
            throw new CompressionException("Not in GZIP format");
        }

        byte[] uncompressedByteArray = strategy.uncompressByteArray(bytes);

        logger.debug("Uncompressing message to size: " + uncompressedByteArray.length);
        return uncompressedByteArray;
    }

    /**
     * Determines if a byte array is compressed.
     *
     * @param bytes an array of bytes
     * @return true if the array is compressed or faluse otherwise
     * @throws java.io.IOException if the byte array couldn't be read
     */
    public static boolean isCompressed(byte[] bytes) throws IOException
    {
        return strategy.isCompressed(bytes);
    }
}
