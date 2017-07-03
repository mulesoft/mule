/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control information for queues.
 * <p/>
 * Provides information about the current write and read file that were used at the moment of the shutdown.
 */
public class QueueControlDataFile {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private static final int INTEGER_SIZE_IN_BYTES = Integer.SIZE / 8;

  private final QueueFileProvider queueFileProvider;
  private File currentReadFilePath;
  private File currentWriteFilePath;

  /**
   * Creates a QueueControlDataFile for storing / retrieving information
   *
   * @param queueFileProvider file provider to use to store control data
   * @param firstFile first queue file. Used for write and read in case there is no control data yet.
   * @param secondFile second queue file.
   */
  public QueueControlDataFile(QueueFileProvider queueFileProvider, File firstFile, File secondFile) {
    this.queueFileProvider = queueFileProvider;
    if (queueFileProvider.isNewFile()) {
      this.currentWriteFilePath = firstFile;
      this.currentReadFilePath = firstFile;
      writeControlData(currentWriteFilePath, currentReadFilePath);
    } else {
      try {
        this.currentWriteFilePath = getStoredFile();
        this.currentReadFilePath = getStoredFile();
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug("failure reading queue control data from file " + queueFileProvider.getFile().getAbsolutePath(), e);
        }
        // perhaps mule crashed while this file was written.
        File lastUpdatedFile = firstFile.lastModified() > secondFile.lastModified() ? firstFile : secondFile;
        this.currentWriteFilePath = lastUpdatedFile;
        this.currentReadFilePath = firstFile == lastUpdatedFile ? secondFile : firstFile;
      }
    }
  }

  /**
   * Updates the control data
   *
   * @param writeFile file that is used for writing
   * @param readFile file that is used for reading
   */
  public void writeControlData(File writeFile, File readFile) {
    try {
      queueFileProvider.getRandomAccessFile().seek(0);
      final String writeFilePath = writeFile.getAbsolutePath();
      final String readFilePath = readFile.getAbsolutePath();
      final ByteBuffer controlDataBuffer =
          ByteBuffer.allocate(writeFilePath.length() + INTEGER_SIZE_IN_BYTES + readFilePath.length() + INTEGER_SIZE_IN_BYTES);
      controlDataBuffer.putInt(writeFilePath.length());
      controlDataBuffer.put(writeFilePath.getBytes());
      controlDataBuffer.putInt(readFilePath.length());
      controlDataBuffer.put(readFilePath.getBytes());
      queueFileProvider.getRandomAccessFile().write(controlDataBuffer.array());
      this.currentReadFilePath = readFile;
      this.currentWriteFilePath = writeFile;
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * @return path of the current file that is used for reading
   */
  public File getCurrentReadFile() {
    return currentReadFilePath;
  }

  /**
   * @return path of the current file that is used for writing
   */
  public File getCurrentWriteFile() {
    return currentWriteFilePath;
  }

  private File getStoredFile() throws IOException {
    return new File(readStringFromFile());
  }

  private String readStringFromFile() throws IOException {
    final int stringSize = queueFileProvider.getRandomAccessFile().readInt();
    final byte[] stringAsBytes = new byte[stringSize];
    queueFileProvider.getRandomAccessFile().read(stringAsBytes);
    return new String(stringAsBytes);
  }

  /**
   * useful for testing
   *
   * @return the QueueFileProvider used for storing the content
   */
  QueueFileProvider getQueueFileProvider() {
    return queueFileProvider;
  }

  public void close() {
    try {
      queueFileProvider.close();
    } catch (IOException e) {
      logger.warn("failure closing queue data control file: " + e.getMessage());
      if (logger.isDebugEnabled()) {
        logger.debug("Failure closing queue data control file", e);
      }
    }
  }

  /**
   * Deletes the underlying file. This method must only be invoked after {@link #close()} has been executed on {@code this}
   * instance
   */
  public void delete() {
    queueFileProvider.delete();
  }
}
