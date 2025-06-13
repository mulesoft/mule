/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.queue;

import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.api.util.Preconditions;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TransactionalQueueStoreDelegate} implementation using two files for storing the queue data.
 * <p/>
 * Entries are stored in the queue file until a certain size in the file. After that size is reached a new file is created and
 * used to store new entries until the previous file queue entries are consumed, in which case the file is cleaned and reused for
 * new entries once the second files gets full.
 */
public class DualRandomAccessFileQueueStoreDelegate extends AbstractQueueStoreDelegate
    implements TransactionalQueueStoreDelegate {

  public static final String MAX_LENGTH_PER_FILE_PROPERTY_KEY = "mule.queue.maxlength";
  private static final int ONE_MEGABYTE = 1024 * 1024;
  private static final String QUEUE_STORE_DIRECTORY = "queuestore";
  private static final Integer MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES =
      Integer.valueOf(System.getProperty(MAX_LENGTH_PER_FILE_PROPERTY_KEY, Integer.valueOf(ONE_MEGABYTE).toString()));
  private static final String QUEUE_STORE_1_SUFFIX = "-1";
  private static final String QUEUE_STORE_2_SUFFIX = "-2";
  private static final Object QUEUE_DATA_CONTROL_SUFFIX = "-crl";

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final SerializationProtocol serializer;
  private final ReadWriteLock filesLock;
  private final QueueControlDataFile queueControlDataFile;
  private RandomAccessFileQueueStore writeFile;
  private RandomAccessFileQueueStore readFile;
  private final RandomAccessFileQueueStore randomAccessFileQueueStore1;
  private final RandomAccessFileQueueStore randomAccessFileQueueStore2;

  public DualRandomAccessFileQueueStoreDelegate(String queueName, String workingDirectory, SerializationProtocol serializer,
                                                int capacity) {
    super(capacity);
    this.serializer = serializer;
    File queuesDirectory = getQueuesDirectory(workingDirectory);
    if (!queuesDirectory.exists()) {
      Preconditions.checkState(queuesDirectory.mkdirs(),
                               "Could not create queue store directory " + queuesDirectory.getAbsolutePath());
    }
    randomAccessFileQueueStore1 =
        new RandomAccessFileQueueStore(new QueueFileProvider(queuesDirectory, queueName + QUEUE_STORE_1_SUFFIX));
    randomAccessFileQueueStore2 =
        new RandomAccessFileQueueStore(new QueueFileProvider(queuesDirectory, queueName + QUEUE_STORE_2_SUFFIX));
    queueControlDataFile = new QueueControlDataFile(new QueueFileProvider(queuesDirectory, queueName + QUEUE_DATA_CONTROL_SUFFIX),
                                                    randomAccessFileQueueStore1.getFile(), randomAccessFileQueueStore2.getFile());
    writeFile = queueControlDataFile.getCurrentWriteFile().getAbsolutePath()
        .equals(randomAccessFileQueueStore1.getFile().getAbsolutePath()) ? randomAccessFileQueueStore1
            : randomAccessFileQueueStore2;
    readFile = queueControlDataFile.getCurrentReadFile().getAbsolutePath()
        .equals(randomAccessFileQueueStore1.getFile().getAbsolutePath()) ? randomAccessFileQueueStore1
            : randomAccessFileQueueStore2;
    filesLock = new ReentrantReadWriteLock();

    logger.debug("Queue {} has {} messages", queueName, getSize());
  }

  // only for testing.
  QueueControlDataFile getQueueControlDataFile() {
    return queueControlDataFile;
  }

  private static File getQueuesDirectory(String workingDirectory) {
    return new File(workingDirectory + File.separator + QUEUE_STORE_DIRECTORY);
  }


  public static File getFirstQueueFileForTesting(String queueName, String workingDirectory) {
    return new File(getQueuesDirectory(workingDirectory), queueName + QUEUE_STORE_1_SUFFIX);
  }

  @Override
  protected void addFirst(Serializable item) throws InterruptedException {
    switchWriteFileIfFull();
    byte[] serialiazedObject = serializer.serialize(item);
    readFile.addFirst(serialiazedObject);
  }

  @Override
  protected void add(Serializable item) {
    switchWriteFileIfFull();
    byte[] serialiazedObject = serializer.serialize(item);
    writeFile.addLast(serialiazedObject);
  }

  @Override
  protected Serializable removeFirst() throws InterruptedException {
    Serializable value = getFirst();
    if (value != null) {
      readFile.removeFirst();
    }
    return value;
  }

  @Override
  protected Serializable getFirst() throws InterruptedException {
    if (isEmpty()) {
      return null;
    }
    Lock lock = filesLock.readLock();
    lock.lock();
    byte[] bytes;
    try {
      if (readFile.isEmpty()) {
        readFile.clear();
        switchReadFile();
      }
      bytes = readFile.getFirst();
    } finally {
      lock.unlock();
    }
    return deserialize(bytes);
  }

  @Override
  public int size() {
    Lock lock = filesLock.readLock();
    lock.lock();
    try {
      return randomAccessFileQueueStore1.getSize() + randomAccessFileQueueStore2.getSize();
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected boolean isEmpty() {
    Lock lock = filesLock.readLock();
    lock.lock();
    try {
      return randomAccessFileQueueStore1.isEmpty() && randomAccessFileQueueStore2.isEmpty();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public synchronized void doClear() {
    Lock lock = filesLock.readLock();
    lock.lock();
    try {
      randomAccessFileQueueStore1.clear();
      randomAccessFileQueueStore2.clear();
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected boolean doAddAll(Collection<? extends Serializable> items) {
    Lock lock = filesLock.readLock();
    lock.lock();
    try {
      for (Serializable item : items) {
        add(item);
      }
    } finally {
      lock.unlock();
    }
    return true;
  }

  public Collection<Serializable> allElements() throws InterruptedException {
    List<Serializable> elements = new LinkedList<>();
    elements.addAll(deserializeValues(randomAccessFileQueueStore1.allElements()));
    elements.addAll(deserializeValues(randomAccessFileQueueStore2.allElements()));
    return elements;
  }

  private Collection<Serializable> deserializeValues(Collection<byte[]> valuesAsBytes) {
    List<Serializable> values = new ArrayList<>(valuesAsBytes.size());
    for (byte[] valueAsByte : valuesAsBytes) {
      try {
        values.add(deserialize(valueAsByte));
      } catch (Exception e) {
        logger.warn("Failure trying to deserialize value " + e.getMessage());
        logger.debug("Failure trying to deserialize value", e);
      }
    }
    return values;
  }

  private Serializable deserialize(byte[] valuesAsBytes) {
    return serializer.deserialize(valuesAsBytes);
  }

  @Override
  public void remove(Serializable value) {
    RawDataSelector rawDataSelector = createDataSelector(value);
    if (!randomAccessFileQueueStore1.remove(rawDataSelector)) {
      randomAccessFileQueueStore2.remove(rawDataSelector);
    }
  }

  private RawDataSelector createDataSelector(final Serializable value) {
    return data -> deserialize(data).equals(value);
  }

  @Override
  public boolean contains(Serializable value) {
    Lock lock = filesLock.readLock();
    lock.lock();
    try {
      final RawDataSelector dataSelector = createDataSelector(value);
      if (!randomAccessFileQueueStore1.contains(dataSelector)) {
        return randomAccessFileQueueStore2.contains(dataSelector);
      }
    } finally {
      lock.unlock();
    }
    return true;
  }

  @Override
  public void close() {
    Lock lock = filesLock.readLock();
    lock.lock();
    try {
      doClose();
    } finally {
      lock.unlock();
    }
  }

  private void switchReadFile() {
    logger.debug("switching read file. Random 1 size: {} , Random 2 size: {}",
                 randomAccessFileQueueStore1.getSize(), randomAccessFileQueueStore2.getSize());
    readFile = nextReadFile();
    queueControlDataFile.writeControlData(writeFile.getFile(), readFile.getFile());
  }

  private void switchWriteFileIfFull() {
    if (writeFile.getLength() >= MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES) {
      Lock lock = filesLock.writeLock();
      lock.lock();
      try {
        if (writeFile.getLength() >= MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES) {
          if (randomAccessFileQueueStore1.getLength() >= MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES
              && randomAccessFileQueueStore2.getLength() >= MAXIMUM_QUEUE_FILE_SIZE_IN_BYTES) {
            return;
          }
          logger.debug("switching write file. Random 1 size: {} , Random 2 size: {}",
                       randomAccessFileQueueStore1.getLength(), randomAccessFileQueueStore2.getLength());
          writeFile = (writeFile == randomAccessFileQueueStore1 ? randomAccessFileQueueStore2 : randomAccessFileQueueStore1);
          queueControlDataFile.writeControlData(writeFile.getFile(), readFile.getFile());
        }
      } finally {
        lock.unlock();
      }
    }
  }

  private RandomAccessFileQueueStore nextReadFile() {
    return readFile == randomAccessFileQueueStore1 ? randomAccessFileQueueStore2 : randomAccessFileQueueStore1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    Lock lock = filesLock.writeLock();
    lock.lock();
    try {
      doClose();
      delete();
    } finally {
      lock.unlock();
    }
  }

  private void delete() {
    Lock lock = filesLock.writeLock();
    lock.lock();
    try {
      randomAccessFileQueueStore1.delete();
      randomAccessFileQueueStore2.delete();
      queueControlDataFile.delete();
    } finally {
      lock.unlock();
    }
  }

  private void doClose() {
    randomAccessFileQueueStore1.close();
    randomAccessFileQueueStore2.close();
    queueControlDataFile.close();
  }

}
