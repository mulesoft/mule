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

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <code>GZipCompression</code> a CompressionStrategy implementation
 * using the GZip library included in the JDK java.util.zip.
 * This is the default CompressionStrategy used by the CompressionHelper
 * discovery when no other implementation is discovered
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
     * Determines if a byte array is compressed. The java.util.zip GZip implementaiton does not expose
     * the GZip header so it is difficult to determine if a string is compressed.
     *
     * @param bytes an array of bytes
     * @return true if the array is compressed or faluse otherwise
     * @throws java.io.IOException if the byte array couldn't be read
     */
    public boolean isCompressed(byte[] bytes) throws IOException
    {
        //We only need the first 2 bytes
        ByteArrayInputStream is = new ByteArrayInputStream(bytes, 0, 2);
        int b = readByte(is);
        b = (readByte(is) << 8) | b;

        return (b == GZIPInputStream.GZIP_MAGIC);
    }

    /**
     * @param in The InputStream to read the byte from
     * @return The byte value
     * @throws java.io.IOException if a byte could not be read
     */
    private int readByte(InputStream in) throws IOException
    {
        int b = in.read();
        if (b == -1)
        {
            throw new EOFException();
        }
        return b;
    }

    /**
     * Used for compressing  a byte array into a new byte array using GZIP
     *
     * @param bytes An array of bytes to compress
     * @return a compressed byte array
     * @throws java.io.IOException if it fails to write to a GZIPOutputStream
     * @see java.util.zip.GZIPOutputStream
     */
    public byte[] compressByteArray(byte[] bytes) throws IOException
    {
        logger.debug("Compressing message of size: " + bytes.length);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(baos);
        //gos.setMethod(ZipOutputStream.DEFLATED);
        gos.write(bytes, 0, bytes.length);
        gos.finish();
        gos.close();
        byte[] compressedByteArray = baos.toByteArray();
        logger.debug("Compressed message to size: " + compressedByteArray.length);
        return compressedByteArray;
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
        logger.debug("Uncompressing message of size: " + bytes.length);
        if (!isCompressed(bytes))
        {
            //throw a specific exception here to allow users of this method to deffientiate between
            //general IOExceptions and an Invalid format
            logger.warn("data is not of type GZIP compressed. The data may not have been compressed in the first place");
            throw new CompressionException("Not in GZIP format");
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        GZIPInputStream gis = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[2048];
        int len;

        while ((len = gis.read(buf)) != -1)
        {
            baos.write(buf, 0, len);
        }

        byte[] uncompressedByteArray = baos.toByteArray();

        logger.debug("Uncompressing message to size: " + uncompressedByteArray.length);
        return uncompressedByteArray;
    }
}