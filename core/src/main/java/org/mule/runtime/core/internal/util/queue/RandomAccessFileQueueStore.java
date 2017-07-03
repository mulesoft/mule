/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic queueing functionality with file storage.
 */
class RandomAccessFileQueueStore {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  static final int CONTROL_DATA_SIZE = 5;

  private static final byte NOT_REMOVED = 0;
  private static final byte REMOVED = 1;
  private final QueueFileProvider queueFileProvider;

  private LinkedList<Long> orderedKeys = new LinkedList<>();
  private long fileTotalSpace = 0;

  public RandomAccessFileQueueStore(QueueFileProvider queueFileProvider) {
    this.queueFileProvider = queueFileProvider;
    initialise();
  }

  /**
   * @return the File where the content is stored.
   */
  public File getFile() {
    return this.queueFileProvider.getFile();
  }

  /**
   * Adds element at the end of the queue.
   *
   * @param element element to add
   */
  public synchronized void addLast(byte[] element) {
    long filePointer = writeData(element);
    orderedKeys.addLast(filePointer);
  }

  /**
   * Remove and returns data from the queue.
   *
   * @return data from the beginning of the queue.
   * @throws InterruptedException
   */
  public synchronized byte[] removeFirst() throws InterruptedException {
    try {
      if (orderedKeys.isEmpty()) {
        return null;
      }
      Long filePosition = orderedKeys.getFirst();
      queueFileProvider.getRandomAccessFile().seek(filePosition);
      queueFileProvider.getRandomAccessFile().writeByte(RandomAccessFileQueueStore.REMOVED);
      byte[] data = readDataInCurrentPosition();
      orderedKeys.removeFirst();
      return data;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves the first element from the queue without removing it.
   *
   * @return first element from the queue.
   * @throws InterruptedException
   */
  public synchronized byte[] getFirst() throws InterruptedException {
    return readFirstValue();
  }

  /**
   * Adds an element in the beginning of the queue.
   *
   * @param item element to add.
   * @throws InterruptedException
   */
  public synchronized void addFirst(byte[] item) throws InterruptedException {
    orderedKeys.addFirst(writeData(item));
  }

  /**
   * @return the size of the queue.
   */
  public int getSize() {
    return orderedKeys.size();
  }

  /**
   * removes all the elements from the queue.
   */
  public synchronized void clear() {
    try {
      queueFileProvider.getRandomAccessFile().close();
      orderedKeys.clear();
      fileTotalSpace = 0;
      queueFileProvider.recreate();
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Adds a collection of elements at the end of the queue.
   *
   * @param items collection of elements to add.
   * @return true if it were able to add them all, false otherwise.
   */
  public synchronized boolean addAll(Collection<? extends byte[]> items) {
    for (byte[] item : items) {
      addLast(item);
    }
    return true;
  }

  /**
   * Use this method carefully since it required bit amount of IO.
   *
   * @return all the elements from the queue.
   */
  public synchronized Collection<byte[]> allElements() {
    List<byte[]> elements = new LinkedList<>();
    try {
      queueFileProvider.getRandomAccessFile().seek(0);
      while (true) {
        boolean removed = queueFileProvider.getRandomAccessFile().readBoolean();
        if (!removed) {
          elements.add(readDataInCurrentPosition());
        } else {
          moveFilePointerToNextData();
        }
      }
    } catch (IOException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Error reading queue elements", e);
      }
    }
    return elements;
  }

  /**
   * @return true if there's no elements in the queue, false otherwise
   */
  public boolean isEmpty() {
    return orderedKeys.isEmpty();
  }

  /**
   * Removes data from the queue according to a {@link RawDataSelector} instance that determines if a certain element must be
   * removed.
   *
   * @param rawDataSelector to determine if the element must be removed.
   * @return true if an element was removed
   */
  public synchronized boolean remove(RawDataSelector rawDataSelector) {
    try {
      queueFileProvider.getRandomAccessFile().seek(0);
      while (true) {
        long currentPosition = queueFileProvider.getRandomAccessFile().getFilePointer();
        byte removed = queueFileProvider.getRandomAccessFile().readByte();
        if (removed == 0) {
          byte[] data = readDataInCurrentPosition();
          if (rawDataSelector.isSelectedData(data)) {
            queueFileProvider.getRandomAccessFile().seek(currentPosition);
            queueFileProvider.getRandomAccessFile().writeByte(REMOVED);
            orderedKeys.remove(currentPosition);
            return true;
          }
        }
      }
    } catch (EOFException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Error removing queue element", e);
      }
      return false;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Free all resources held for the queue.
   * <p/>
   * Do not removes elements from the queue.
   */
  public synchronized void close() {
    try {
      this.queueFileProvider.close();
    } catch (IOException e) {
      logAndIgnore(e);
    }
  }

  private void logAndIgnore(IOException e) {
    logger.warn(e.getMessage());
    if (logger.isDebugEnabled()) {
      logger.debug("Error closing queue store", e);
    }
  }

  /**
   * Deletes the files backing this queue. This method must only be invoked after {@link #close()} has been executed on
   * {@code this} instance
   */
  public synchronized void delete() {
    queueFileProvider.delete();
  }

  private byte[] readDataInCurrentPosition() throws IOException {
    int serializedValueSize = queueFileProvider.getRandomAccessFile().readInt();
    byte[] data = new byte[serializedValueSize];
    queueFileProvider.getRandomAccessFile().read(data, 0, serializedValueSize);
    return data;
  }

  private long writeData(byte[] data) {
    try {
      if (getSize() > 0) {
        queueFileProvider.getRandomAccessFile().seek(fileTotalSpace);
      }
      long filePointer = queueFileProvider.getRandomAccessFile().getFilePointer();
      int totalBytesRequired = CONTROL_DATA_SIZE + data.length;
      ByteBuffer byteBuffer = ByteBuffer.allocate(totalBytesRequired);
      byteBuffer.put(NOT_REMOVED);
      byteBuffer.putInt(data.length);
      byteBuffer.put(data);
      queueFileProvider.getRandomAccessFile().write(byteBuffer.array());
      fileTotalSpace += totalBytesRequired;
      return filePointer;
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private void initialise() {
    try {
      queueFileProvider.getRandomAccessFile().seek(0);
      while (true) {
        long position = queueFileProvider.getRandomAccessFile().getFilePointer();
        byte removed = queueFileProvider.getRandomAccessFile().readByte();
        if (removed == NOT_REMOVED) {
          orderedKeys.add(position);
          moveFilePointerToNextData();
        } else {
          moveFilePointerToNextData();
        }
      }
    } catch (EOFException e) {
      try {
        fileTotalSpace = queueFileProvider.getRandomAccessFile().length();
      } catch (IOException ioe) {
        throw new MuleRuntimeException(e);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Error initializing queue store", e);
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private byte[] readFirstValue() {
    try {
      if (orderedKeys.isEmpty()) {
        return null;
      }
      Long filePointer = orderedKeys.getFirst();
      queueFileProvider.getRandomAccessFile().seek(filePointer);
      queueFileProvider.getRandomAccessFile().readByte(); // Always true since it's a key
      return readDataInCurrentPosition();
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private void moveFilePointerToNextData() throws IOException {
    int serializedValueSize = queueFileProvider.getRandomAccessFile().readInt();
    queueFileProvider.getRandomAccessFile().seek(queueFileProvider.getRandomAccessFile().getFilePointer() + serializedValueSize);
  }

  /**
   * @return the length of the file in bytes.
   */
  public long getLength() {
    return fileTotalSpace;
  }

  /**
   * Searches for data within the queue store using a {@link RawDataSelector}
   *
   * @param rawDataSelector to determine if the element is the one we are looking for
   * @return true if an element exists within the queue, false otherwise
   */
  public synchronized boolean contains(RawDataSelector rawDataSelector) {
    try {
      queueFileProvider.getRandomAccessFile().seek(0);
      while (true) {
        byte removed = queueFileProvider.getRandomAccessFile().readByte();
        if (removed == NOT_REMOVED) {
          byte[] data = readDataInCurrentPosition();
          if (rawDataSelector.isSelectedData(data)) {
            return true;
          }
        } else {
          moveFilePointerToNextData();
        }
      }
    } catch (EOFException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cannot determine if element is contained in the queue store", e);
      }
      return false;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
