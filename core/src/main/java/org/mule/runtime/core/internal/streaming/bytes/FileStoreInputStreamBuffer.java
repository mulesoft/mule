/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Long.MIN_VALUE;
import static java.lang.Math.abs;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.nio.ByteBuffer.allocate;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.streaming.bytes.FileStoreCursorStreamConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link InputStreamBuffer} which is capable of handling datasets larger than this buffer's size. It works by keeping
 * an in-memory buffer which holds as many information as possible. When information which is ahead
 * of the buffer's current position is requested then the following happens:
 * <p>
 * <ul>
 * <li>The contents of the buffer are written into a temporal file</li>
 * <li>The buffer is cleared</li>
 * <li>The stream is consumed until the buffer is full again or the stream reaches its end</li>
 * <li>If the required data is still ahead of the buffer, then the process is repeated until the data is reached or the stream
 * fully consumed</li>
 * </ul>
 * <p>
 * Another possible scenario, is one in which the data requested is behind the buffer's current position, in which case
 * the data is obtained by reading the temporal file.
 * <p>
 * In either case, what's really important to understand is that the buffer is <b>ALWAYS</b> moving forward. The buffer
 * will never go back and reload data from the temporal file. It only gets data from the stream.
 *
 * @since 4.0
 */
public final class FileStoreInputStreamBuffer extends AbstractInputStreamBuffer {

  private static final String TEMP_DIR_SYSTEM_PROPERTY = "java.io.tmpdir";
  private static final File TEMP_DIR = new File(getProperty(TEMP_DIR_SYSTEM_PROPERTY));
  private static Random RANDOM = new SecureRandom();

  private final File bufferFile;
  private final RandomAccessFile fileStore;
  private final Lock fileStoreLock = new ReentrantLock();
  private final ScheduledExecutorService executorService;

  /**
   * Creates a new instance
   *
   * @param stream          the stream to buffer from
   * @param streamChannel   the channel from which reading the {@code stream}
   * @param config          this buffer's configuration
   * @param alreadyFetched  a buffer with information which has already been buffered from the stream. It will be added to this
   *                        buffer's contents
   * @param executorService a {@link ScheduledExecutorService} for performing asynchronous tasks
   */
  public FileStoreInputStreamBuffer(InputStream stream,
                                    ReadableByteChannel streamChannel,
                                    FileStoreCursorStreamConfig config,
                                    ByteBuffer alreadyFetched,
                                    ScheduledExecutorService executorService) {

    super(stream, streamChannel, config.getMaxInMemorySize().toBytes());
    this.executorService = executorService;
    bufferFile = createBufferFile("stream-buffer");
    try {
      fileStore = new RandomAccessFile(bufferFile, "rw");
    } catch (FileNotFoundException e) {
      throw new RuntimeException(format("Buffer file %s was just created but now it doesn't exist",
                                        bufferFile.getAbsolutePath()));
    }
    if (alreadyFetched != null) {
      consume(alreadyFetched);
      alreadyFetched.position(0);
      persistInFileStore(alreadyFetched);
    }
  }

  /**
   * {@inheritDoc}
   * Obtains information which has already been buffered and stored into the file store.
   */
  @Override
  protected int getBackwardsData(ByteBuffer dest, Range requiredRange, int length) {
    releaseBufferLock();
    return checked(() -> {
      // why did I need this buffer?
      ByteBuffer buffer = allocate(length);
      return withFileLock(() -> {
        int read = fileStore.getChannel().read(buffer, requiredRange.start);
        if (read > 0) {
          buffer.flip();
          dest.put(buffer);
        }
        return read;
      });
    });
  }

  /**
   * {@inheritDoc}
   * This buffer cannot be expanded since the in-memory buffer is a rolling one and the file will grow indefinitively.
   *
   * @return {@code false}
   */
  @Override
  protected boolean canBeExpanded() {
    return false;
  }

  /**
   * {@inheritDoc}
   * If the required position is on disk, then it will be retrieved from there. Otherwise, the stream will be consumed.
   * All the data obtained will be written into disk
   */
  @Override
  protected int consumeForwardData(ByteBuffer buffer) throws IOException {
    buffer.clear();
    int result = reloadFromFileStore(buffer);

    if (result > 0) {
      buffer.flip();
      return result;
    }

    result = loadFromStream(buffer);

    if (result >= 0) {
      buffer.flip();
      if (persistInFileStore(buffer)) {
        buffer.flip();
      }
    }

    return result;
  }

  private Integer reloadFromFileStore(ByteBuffer buffer) {
    return checked(() -> withFileLock(() -> fileStore.getChannel().read(buffer)));
  }

  private boolean persistInFileStore(ByteBuffer buffer) {
    try {
      withFileLock(() -> fileStore.getChannel().write(buffer));
      return true;
    } catch (IOException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not write in off-heap file store"), e);
    }
  }

  private <T> T checked(Callable<T> callable) {
    try {
      return callable.call();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not read from file store"), e);
    }
  }

  private <T> T withFileLock(Callable<T> callable) throws IOException {
    fileStoreLock.lock();
    try {
      return callable.call();
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    } finally {
      fileStoreLock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doClose() {
    closeQuietly(fileStore);
    executorService.submit(bufferFile::delete);
  }

  private File createBufferFile(String name) {
    return createTempFile("mule-buffer-${" + name + "}-", ".tmp");
  }

  private File createTempFile(String prefix, String suffix) throws RuntimeException {
    long n = RANDOM.nextLong();
    if (n == MIN_VALUE) {
      n = 0;
    } else {
      n = abs(n);
    }

    if (!TEMP_DIR.exists()) {
      throw new RuntimeException(format("Temp directory '%s' does not exits. Please check the value of the '%s' system property.",
                                        TEMP_DIR.getAbsolutePath(),
                                        TEMP_DIR_SYSTEM_PROPERTY));
    }
    return new File(TEMP_DIR, prefix + n + suffix);
  }
}
