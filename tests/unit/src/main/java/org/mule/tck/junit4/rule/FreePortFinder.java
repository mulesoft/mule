/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import static java.nio.channels.FileChannel.open;
import static java.nio.file.Paths.get;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.apache.commons.lang3.SystemUtils.PATH_SEPARATOR;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Finds available port numbers in a specified range.
 */
public class FreePortFinder {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final int minPortNumber;
  private final int portRange;
  private final Random random = new Random();
  private final String LOCK_FILE_EXTENSION = ".lock";
  private final Map<Integer, FileLock> locks = new HashMap<>();
  private final Map<Integer, FileChannel> files = new HashMap<>();
  private final String basePath = System.getProperty("maven.multiModuleProjectDirectory", ".");

  public FreePortFinder(int minPortNumber, int maxPortNumber) {
    this.minPortNumber = minPortNumber;
    this.portRange = maxPortNumber - minPortNumber;
  }

  public synchronized Integer find() {
    for (int i = 0; i < portRange; i++) {
      int port = minPortNumber + random.nextInt(portRange);
      String portFile = port + LOCK_FILE_EXTENSION;
      try {
        FileChannel channel = open(get(basePath + PATH_SEPARATOR + portFile), CREATE, WRITE, DELETE_ON_CLOSE);
        FileLock lock = channel.tryLock();
        if (lock == null) {
          // If the lock couldn't be acquired and tryLock didn't throw the exception, we throw it here
          throw new OverlappingFileLockException();
        }

        if (isPortFree(port)) {
          if (logger.isDebugEnabled()) {
            logger.debug("Found free port: " + port);
          }

          locks.put(port, lock);
          files.put(port, channel);
          return port;
        } else {
          lock.release();
          channel.close();
        }
      } catch (OverlappingFileLockException e) {
        // The file is locked,
        if (logger.isDebugEnabled()) {
          logger.debug("Port selected already locked");
        }
      } catch (IOException e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Error when trying to open port lock file, trying another port");
        }
      }

    }

    throw new IllegalStateException("Unable to find an available port");
  }

  /**
   * Indicates that the port is free from the point of view of the caller.
   * <p/>
   * Checks that the port was released, if it was not, then it would be
   * marked as in use, so no other client receives the same port again.
   *
   * @param port the port number to release.
   */
  public synchronized void releasePort(int port) {
    if (isPortFree(port) && locks.containsKey(port) && files.containsKey(port)) {
      FileLock lock = locks.remove(port);
      FileChannel file = files.remove(port);
      try {
        lock.release();
        file.close();

      } catch (IOException e) {
        // Ignore
      }
    } else {
      if (logger.isInfoEnabled()) {
        logger.info(String.format("Port %d was not correctly released", port));
      }
    }
  }

  /**
   * Check and log is a given port is available
   *
   * @param port the port number to check
   * @return true if the port is available, false otherwise
   */
  public boolean isPortFree(int port) {
    boolean portIsFree = true;

    ServerSocket server = null;
    try {
      server = new ServerSocket(port);
      server.setReuseAddress(true);
    } catch (IOException e) {
      portIsFree = false;
    } finally {
      if (server != null) {
        try {
          server.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }

    return portIsFree;
  }
}
