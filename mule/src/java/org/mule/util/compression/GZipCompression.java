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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <code>GZipCompression</code> a CompressionStrategy implementation using the
 * GZip library included in the JDK java.util.zip. This is the default
 * CompressionStrategy used by the CompressionHelper discovery when no other
 * implementation is discovered
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GZipCompression implements CompressionStrategy
{
    /**
     * The logger for this class
     */
    private static final transient Log logger = LogFactory.getLog(GZipCompression.class);

    /**
     * Determines if a byte array is compressed. The java.util.zip GZip
     * implementaiton does not expose the GZip header so it is difficult to
     * determine if a string is compressed.
     * 
     * @param bytes
     *            an array of bytes
     * @return true if the array is compressed or false otherwise
     * @throws java.io.IOException
     *             if the byte array couldn't be read
     */
    public boolean isCompressed(byte[] bytes) throws IOException
    {
        if ((bytes == null) || (bytes.length < 2)) {
            return false;
        }
        else {
            return ((bytes[0] == (byte)(GZIPInputStream.GZIP_MAGIC)) && (bytes[1] == (byte)(GZIPInputStream.GZIP_MAGIC >> 8)));
        }
    }

    /**
     * Used for compressing a byte array into a new byte array using GZIP
     * 
     * @param bytes
     *            An array of bytes to compress
     * @return a compressed byte array
     * @throws java.io.IOException
     *             if it fails to write to a GZIPOutputStream
     * @see java.util.zip.GZIPOutputStream
     */
    public byte[] compressByteArray(byte[] bytes) throws IOException
    {
        if (bytes == null || isCompressed(bytes)) {
            // nothing to compress
            return bytes;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Compressing message of size: " + bytes.length);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(baos);

        gos.write(bytes, 0, bytes.length);
        gos.finish();
        gos.close();

        byte[] compressedByteArray = baos.toByteArray();

        if (logger.isDebugEnabled()) {
            logger.debug("Compressed message to size: " + compressedByteArray.length);
        }

        return compressedByteArray;
    }

    /**
     * Used for uncompressing a byte array into a uncompressed byte array using
     * GZIP
     * 
     * @param bytes
     *            An array of bytes to uncompress
     * @return an uncompressed byte array
     * @throws java.io.IOException
     *             if it fails to read from a GZIPInputStream
     * @see java.util.zip.GZIPInputStream
     */
    public byte[] uncompressByteArray(byte[] bytes) throws IOException
    {
        if (!isCompressed(bytes)) {
            // nothing to uncompress
            return bytes;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Uncompressing message of size: " + bytes.length);
        }

        if (!isCompressed(bytes)) {
            // throw a specific exception here to allow users of this method to
            // diffientiate between general IOExceptions and an invalid format
            logger.warn("Data is not of type GZIP compressed."
                    + " The data may not have been compressed in the first place.");
            throw new CompressionException("Not in GZIP format");
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        GZIPInputStream gis = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length * 4);

        IOUtils.copy(gis, baos);
        byte[] uncompressedByteArray = baos.toByteArray();

        if (logger.isDebugEnabled()) {
            logger.debug("Uncompressed message to size: " + uncompressedByteArray.length);
        }

        return uncompressedByteArray;
    }

}
