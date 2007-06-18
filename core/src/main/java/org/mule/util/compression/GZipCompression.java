/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.compression;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>GZipCompression</code> is a CompressionStrategy implementation using the
 * GZip library included in the JDK java.util.zip. This is the default
 * CompressionStrategy used by the CompressionHelper discovery when no other
 * implementation is discovered.
 */
public class GZipCompression implements CompressionStrategy
{
    public static final int DEFAULT_BUFFER_SIZE = 32768;
    
    /**
     * The logger for this class
     */
    private static final Log logger = LogFactory.getLog(GZipCompression.class);

    /**
     * Determines if a byte array is compressed. The java.util.zip GZip
     * implementaiton does not expose the GZip header so it is difficult to determine
     * if a string is compressed.
     * 
     * @param bytes an array of bytes
     * @return true if the array is compressed or false otherwise
     * @throws java.io.IOException if the byte array couldn't be read
     */
    public boolean isCompressed(byte[] bytes) throws IOException
    {
        if ((bytes == null) || (bytes.length < 2))
        {
            return false;
        }
        else
        {
            return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
        }
    }

    /**
     * Used for compressing a byte array into a new byte array using GZIP
     * 
     * @param bytes An array of bytes to compress
     * @return a compressed byte array
     * @throws java.io.IOException if it fails to write to a GZIPOutputStream
     * @see java.util.zip.GZIPOutputStream
     */
    public byte[] compressByteArray(byte[] bytes) throws IOException
    {
        // TODO add strict behaviour as option
        if (bytes == null || isCompressed(bytes))
        {
            // nothing to compress
            if (logger.isDebugEnabled())
            {
                logger.debug("Data already compressed; doing nothing");
            }
            return bytes;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Compressing message of size: " + bytes.length);
        }

        ByteArrayOutputStream baos = null;
        GZIPOutputStream gzos = null;

        try
        {
            baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
            gzos = new GZIPOutputStream(baos);

            gzos.write(bytes, 0, bytes.length);
            gzos.finish();
            gzos.close();

            byte[] compressedByteArray = baos.toByteArray();
            baos.close();

            if (logger.isDebugEnabled())
            {
                logger.debug("Compressed message to size: " + compressedByteArray.length);
            }

            return compressedByteArray;
        }
        catch (IOException ioex)
        {
            throw ioex;
        }
        finally
        {
            IOUtils.closeQuietly(gzos);
            IOUtils.closeQuietly(baos);
        }
    }

    /**
     * Used for uncompressing a byte array into a uncompressed byte array using GZIP
     * 
     * @param bytes An array of bytes to uncompress
     * @return an uncompressed byte array
     * @throws java.io.IOException if it fails to read from a GZIPInputStream
     * @see java.util.zip.GZIPInputStream
     */
    public byte[] uncompressByteArray(byte[] bytes) throws IOException
    {
        // TODO add strict behaviour as option
        if (!isCompressed(bytes))
        {
            /*
             * if (strict) { // throw a specific exception here to allow users of
             * this method to // diffientiate between base IOExceptions and an
             * invalid format logger.warn("Data is not of type GZIP compressed." + "
             * The data may not have been compressed in the first place."); throw new
             * CompressionException("Not in GZIP format"); }
             */

            // nothing to uncompress
            if (logger.isDebugEnabled())
            {
                logger.debug("Data already uncompressed; doing nothing");
            }
            return bytes;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Uncompressing message of size: " + bytes.length);
        }

        ByteArrayInputStream bais = null;
        GZIPInputStream gzis = null;
        ByteArrayOutputStream baos = null;

        try
        {
            bais = new ByteArrayInputStream(bytes);
            gzis = new GZIPInputStream(bais);
            baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);

            IOUtils.copy(gzis, baos);
            gzis.close();
            bais.close();

            byte[] uncompressedByteArray = baos.toByteArray();
            baos.close();

            if (logger.isDebugEnabled())
            {
                logger.debug("Uncompressed message to size: " + uncompressedByteArray.length);
            }

            return uncompressedByteArray;
        }
        catch (IOException ioex)
        {
            throw ioex;
        }
        finally
        {
            IOUtils.closeQuietly(gzis);
            IOUtils.closeQuietly(bais);
            IOUtils.closeQuietly(baos);
        }
    }

}
