/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.compression;

import static org.apache.commons.io.IOUtils.copy;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;

import org.mule.runtime.core.internal.transformer.compression.GZIPCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>GZipCompression</code> is a CompressionStrategy implementation using the GZip library included in the JDK java.util.zip.
 */
public class GZipCompression implements CompressionStrategy {

  public static final int DEFAULT_BUFFER_SIZE = 32768;

  /**
   * The logger for this class
   */
  private static final Logger logger = LoggerFactory.getLogger(GZipCompression.class);

  /**
   * Determines if a byte array is compressed. The java.util.zip GZip implementaiton does not expose the GZip header so it is
   * difficult to determine if a string is compressed.
   * 
   * @param bytes an array of bytes
   * @return true if the array is compressed or false otherwise
   * @throws java.io.IOException if the byte array couldn't be read
   */
  public boolean isCompressed(byte[] bytes) throws IOException {
    if ((bytes == null) || (bytes.length < 2)) {
      return false;
    } else {
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
  public byte[] compressByteArray(byte[] bytes) throws IOException {
    // TODO add strict behaviour as option
    if (bytes == null || isCompressed(bytes)) {
      // nothing to compress
      if (logger.isDebugEnabled()) {
        logger.debug("Data already compressed; doing nothing");
      }
      return bytes;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Compressing message of size: " + bytes.length);
    }

    ByteArrayOutputStream baos = null;
    GZIPOutputStream gzos = null;

    try {
      baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
      gzos = new GZIPOutputStream(baos);

      gzos.write(bytes, 0, bytes.length);
      gzos.finish();
      gzos.close();

      byte[] compressedByteArray = baos.toByteArray();
      baos.close();

      if (logger.isDebugEnabled()) {
        logger.debug("Compressed message to size: " + compressedByteArray.length);
      }

      return compressedByteArray;
    } catch (IOException ioex) {
      throw ioex;
    } finally {
      closeQuietly(gzos);
      closeQuietly(baos);
    }
  }

  public InputStream compressInputStream(InputStream is) throws IOException {
    return new GZIPCompressorInputStream(is);
  }

  /**
   * Used for uncompressing a byte array into a uncompressed byte array using GZIP
   * 
   * @param bytes An array of bytes to uncompress
   * @return an uncompressed byte array
   * @throws java.io.IOException if it fails to read from a GZIPInputStream
   * @see java.util.zip.GZIPInputStream
   */
  public byte[] uncompressByteArray(byte[] bytes) throws IOException {
    if (!isCompressed(bytes)) {
      // nothing to uncompress
      if (logger.isDebugEnabled()) {
        logger.debug("Data already uncompressed; doing nothing");
      }
      return bytes;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Uncompressing message of size: " + bytes.length);
    }

    ByteArrayInputStream bais = null;
    GZIPInputStream gzis = null;
    ByteArrayOutputStream baos = null;

    try {
      bais = new ByteArrayInputStream(bytes);
      gzis = new GZIPInputStream(bais);
      baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);

      copy(gzis, baos);
      gzis.close();
      bais.close();

      byte[] uncompressedByteArray = baos.toByteArray();
      baos.close();

      if (logger.isDebugEnabled()) {
        logger.debug("Uncompressed message to size: " + uncompressedByteArray.length);
      }

      return uncompressedByteArray;
    } catch (IOException ioex) {
      throw ioex;
    } finally {
      closeQuietly(gzis);
      closeQuietly(bais);
      closeQuietly(baos);
    }
  }

  public InputStream uncompressInputStream(InputStream is) throws IOException {
    return new GZIPInputStream(is);
  }

}
