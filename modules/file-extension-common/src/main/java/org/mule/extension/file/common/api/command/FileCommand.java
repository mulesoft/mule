/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.command;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.exceptions.FileAlreadyExistsException;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

import org.slf4j.Logger;

/**
 * Base class for implementations of the Command design pattern which performs operations on a file system
 *
 * @param <F> the generic type of the {@link FileSystem} on which the operation is performed
 * @since 4.0
 */
public abstract class FileCommand<F extends FileSystem> {

  private static final Logger LOGGER = getLogger(FileCommand.class);

  protected final F fileSystem;

  /**
   * Creates a new instance
   *
   * @param fileSystem the {@link FileSystem} on which the operation is performed
   */
  protected FileCommand(F fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * Returns true if the given {@code path} exists
   *
   * @param path the {@link Path} to test
   * @return whether the {@code path} exists
   */
  protected abstract boolean exists(Path path);

  protected void assureParentFolderExists(Path path, boolean createParentFolder) {
    if (exists(path)) {
      return;
    }

    Path parentFolder = path.getParent();
    if (!exists(parentFolder)) {
      if (createParentFolder) {
        mkdirs(parentFolder);
      } else {
        throw new IllegalPathException(format("Cannot write to file '%s' because path to it doesn't exist. Consider setting the 'createParentFolder' attribute to 'true'",
                                              path));
      }
    }
  }

  /**
   * Creates the directory pointed by {@code directoryPath} also creating any missing parent directories
   *
   * @param directoryPath the {@link Path} to the directory you want to create
   */
  protected final void mkdirs(Path directoryPath) {
    Lock lock = fileSystem.createMuleLock(format("%s-mkdirs-%s", getClass().getName(), directoryPath));
    lock.lock();
    try {
      // verify no other thread beat us to it
      if (exists(directoryPath)) {
        return;
      }
      doMkDirs(directoryPath);
    } finally {
      lock.unlock();
    }

    LOGGER.debug("Directory '{}' created", directoryPath);
  }

  protected abstract void doMkDirs(Path directoryPath);

  /**
   * Returns an absolute {@link Path} to the given {@code filePath}
   *
   * @param filePath the path to a file or directory
   * @return an absolute {@link Path}
   */
  protected Path resolvePath(String filePath) {
    Path path = getBasePath(fileSystem);
    if (filePath != null) {
      path = path.resolve(filePath);
    }

    return path.toAbsolutePath();
  }

  /**
   * Returns a {@link Path} to which all non absolute paths are relative to
   *
   * @param fileSystem the file system that we're connecting to
   * @return a not {@code null} {@link Path}
   */
  protected abstract Path getBasePath(FileSystem fileSystem);

  /**
   * Similar to {@link #resolvePath(String)} only that it throws a {@link IllegalArgumentException} if the
   * given path doesn't exists.
   * <p>
   * The existence of the obtained path is verified by delegating into {@link #exists(Path)}
   *
   * @param filePath the path to a file or directory
   * @return an absolute {@link Path}
   */
  protected Path resolveExistingPath(String filePath) {
    Path path = resolvePath(filePath);
    if (!exists(path)) {
      throw pathNotFoundException(path);
    }

    return path;
  }

  /**
   * Returns a properly formatted {@link MuleRuntimeException} for the given {@code message} and {@code cause}
   *
   * @param message the exception's message
   * @return a {@link RuntimeException}
   */
  public RuntimeException exception(String message) {
    return new MuleRuntimeException(createStaticMessage(message));
  }

  /**
   * Returns a properly formatted {@link MuleRuntimeException} for the given {@code message} and {@code cause}
   *
   * @param message the exception's message
   * @param cause the exception's cause
   * @return {@link RuntimeException}
   */
  public RuntimeException exception(String message, Exception cause) {
    return new MuleRuntimeException(createStaticMessage(message), cause);
  }

  /**
   * @param fileName the name of a file
   * @return {@code true} if {@code fileName} equals to &quot;.&quot; or &quot;..&quot;
   */
  protected boolean isVirtualDirectory(String fileName) {
    return ".".equals(fileName) || "..".equals(fileName);
  }


  /**
   * Returns an {@link IllegalArgumentException} explaining that a
   * {@link FileSystem#read(FileConnectorConfig, Message, String, boolean)} operation was attempted on a {@code path} pointing to
   * a directory
   *
   * @param path the {@link Path} on which a read was attempted
   * @return {@link RuntimeException}
   */
  protected RuntimeException cannotReadDirectoryException(Path path) {
    return new IllegalPathException(format("Cannot read path '%s' since it's a directory", path));
  }

  /**
   * Returns a {@link IllegalArgumentException} explaining that a
   * {@link FileSystem#list(FileConnectorConfig, String, boolean, Message, Predicate)} operation was attempted on a {@code path}
   * pointing to a file.
   *
   * @param path the {@link Path} on which a list was attempted
   * @return {@link RuntimeException}
   */
  protected RuntimeException cannotListFileException(Path path) {
    return new IllegalPathException(format("Cannot list path '%s' because it's a file. Only directories can be listed",
                                           path));
  }

  /**
   * Returns a {@link IllegalArgumentException} explaining that a
   * {@link FileSystem#list(FileConnectorConfig, String, boolean, Message, Predicate)} operation was attempted on a {@code path}
   * pointing to a file.
   *
   * @param path the {@link Path} on which a list was attempted
   * @return {@link RuntimeException}
   */
  protected RuntimeException pathNotFoundException(Path path) {
    return new IllegalPathException(format("Path '%s' doesn't exists", path));
  }

  /**
   * Returns a {@link IllegalArgumentException} explaining that an operation is trying to write to the given {@code path} but it
   * already exists and no overwrite instruction was provided.
   *
   * @param path the {@link Path} that the operation tried to modify
   * @return {@link RuntimeException}
   */
  public FileAlreadyExistsException alreadyExistsException(Path path) {
    return new FileAlreadyExistsException(format("'%s' already exists. Set the 'overwrite' parameter to 'true' to perform the operation anyway",
                                                 path));
  }
}
