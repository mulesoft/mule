/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

import static java.lang.String.format;

import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.runtime.module.extension.file.api.command.CopyCommand;
import org.mule.runtime.module.extension.file.api.command.CreateDirectoryCommand;
import org.mule.runtime.module.extension.file.api.command.DeleteCommand;
import org.mule.runtime.module.extension.file.api.command.ListCommand;
import org.mule.runtime.module.extension.file.api.command.MoveCommand;
import org.mule.runtime.module.extension.file.api.command.ReadCommand;
import org.mule.runtime.module.extension.file.api.command.RenameCommand;
import org.mule.runtime.module.extension.file.api.command.WriteCommand;
import org.mule.runtime.module.extension.file.api.lock.PathLock;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;

/**
 * Base class for implementations of {@link FileSystem}
 *
 * @since 4.0
 */
public abstract class AbstractFileSystem implements FileSystem {

  private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

  @Inject
  private MuleContext muleContext;

  /**
   * @return a {@link ListCommand}
   */
  protected abstract ListCommand getListCommand();

  /**
   * @return a {@link ReadCommand}
   */
  protected abstract ReadCommand getReadCommand();

  /**
   * @return a {@link WriteCommand}
   */
  protected abstract WriteCommand getWriteCommand();

  /**
   * @return a {@link CopyCommand}
   */
  protected abstract CopyCommand getCopyCommand();

  /**
   * @return a {@link MoveCommand}
   */
  protected abstract MoveCommand getMoveCommand();

  /**
   * @return a {@link DeleteCommand}
   */
  protected abstract DeleteCommand getDeleteCommand();

  /**
   * @return a {@link RenameCommand}
   */
  protected abstract RenameCommand getRenameCommand();

  /**
   * @return a {@link CreateDirectoryCommand}
   */
  protected abstract CreateDirectoryCommand getCreateDirectoryCommand();

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeNode list(FileConnectorConfig config, String directoryPath, boolean recursive, MuleMessage message,
                       Predicate<FileAttributes> matcher) {
    return getListCommand().list(config, directoryPath, recursive, message, matcher);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationResult<InputStream, FileAttributes> read(FileConnectorConfig config, MuleMessage message, String filePath,
                                                           boolean lock) {
    return getReadCommand().read(config, message, filePath, lock);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(FileConnectorConfig config, String filePath, Object content, FileWriteMode mode, MuleEvent event,
                    boolean lock, boolean createParentDirectories, String encoding) {
    getWriteCommand().write(config, filePath, content, mode, event, lock, createParentDirectories, encoding);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void copy(FileConnectorConfig config, String sourcePath, String targetDirectory, boolean overwrite,
                   boolean createParentDirectories, MuleEvent event) {
    getCopyCommand().copy(config, sourcePath, targetDirectory, overwrite, createParentDirectories, event);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void move(FileConnectorConfig config, String sourcePath, String targetDirectory, boolean overwrite,
                   boolean createParentDirectories) {
    getMoveCommand().move(config, sourcePath, targetDirectory, overwrite, createParentDirectories);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(FileConnectorConfig config, String filePath) {
    getDeleteCommand().delete(config, filePath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void rename(FileConnectorConfig config, String filePath, String newName, boolean overwrite) {
    getRenameCommand().rename(config, filePath, newName, overwrite);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createDirectory(FileConnectorConfig config, String directoryName) {
    getCreateDirectoryCommand().createDirectory(config, directoryName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final synchronized PathLock lock(Path path, Object... params) {
    PathLock lock = createLock(path, params);
    if (!lock.tryLock()) {
      throw new IllegalStateException(format("Could not lock file '%s' because it's already owned by another process", path));
    }

    return lock;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MediaType getFileMessageMediaType(MediaType originalMediaType, FileAttributes attributes) {
    MediaType presumedMimeType = MediaType.parse(mimetypesFileTypeMap.getContentType(attributes.getPath()));
    MediaType mediaType = presumedMimeType != null ? presumedMimeType : originalMediaType;
    return originalMediaType.getCharset().map(charset -> mediaType.withCharset(charset)).orElse(mediaType);
  }

  /**
   * Try to acquire a lock on a file and release it immediately. Usually used as a quick check to see if another process is still
   * holding onto the file, e.g. a large file (more than 100MB) is still being written to.
   */
  protected boolean isLocked(Path path) {
    PathLock lock = createLock(path);
    try {
      return !lock.tryLock();
    } finally {
      lock.release();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void verifyNotLocked(Path path) {
    if (isLocked(path)) {
      throw new IllegalStateException(format("File '%s' is locked by another process", path));
    }
  }

  protected abstract PathLock createLock(Path path, Object... params);

  /**
   * {@inheritDoc}
   */
  @Override
  public Lock createMuleLock(String lockId) {
    return muleContext.getLockFactory().createLock(lockId);
  }
}
