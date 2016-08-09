/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import static java.lang.String.format;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.internal.AbstractFtpCopyDelegate;
import org.mule.extension.ftp.internal.FtpCopyDelegate;
import org.mule.extension.ftp.internal.ftp.connection.FtpFileSystem;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.FileCommand;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link FileCommand} implementations that target a FTP/SFTP server
 *
 * @param <Connection> the generic type of the connection object
 * @since 4.0
 */
public abstract class FtpCommand<Connection extends FtpFileSystem> extends FileCommand<Connection> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpCommand.class);

  /**
   * Creates a new instance
   *
   * @param fileSystem a {@link FtpFileSystem} used as the connection object
   */
  public FtpCommand(Connection fileSystem) {
    super(fileSystem);
  }

  /**
   * Similar to {@link #getFile(FileConnectorConfig, String)} but throwing an {@link IllegalArgumentException} if the
   * {@code filePath} doesn't exists
   *
   * @param config the config that is parameterizing this operation
   * @param filePath the path to the file you want
   * @return a {@link FtpFileAttributes}
   * @throws IllegalArgumentException if the {@code filePath} doesn't exists
   */
  protected FtpFileAttributes getExistingFile(FileConnectorConfig config, String filePath) {
    return getFile(config, filePath, true);
  }

  /**
   * Obtains a {@link FtpFileAttributes} for the given {@code filePath} by using the {@link FTPClient#mlistFile(String)} FTP
   * command
   *
   * @param config the config that is parameterizing this operation
   * @param filePath the path to the file you want
   * @return a {@link FtpFileAttributes} or {@code null} if it doesn't exists
   */
  public FtpFileAttributes getFile(FileConnectorConfig config, String filePath) {
    return getFile(config, filePath, false);
  }

  protected abstract FtpFileAttributes getFile(FileConnectorConfig config, String filePath, boolean requireExistence);

  /**
   * {@inheritDoc}
   */
  protected boolean exists(FileConnectorConfig config, Path path) {
    return getFile(config, path.toString()) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Path getBasePath(FileConnectorConfig config) {
    return Paths.get(getCurrentWorkingDirectory());
  }

  /**
   * Changes the current working directory to the given {@code path}
   *
   * @param path the {@link Path} to which you wish to move
   * @throws IllegalArgumentException if the CWD could not be changed
   */
  protected void changeWorkingDirectory(Path path) {
    changeWorkingDirectory(path.toString());
  }

  /**
   * Changes the current working directory to the given {@code path}
   *
   * @param path the path to which you wish to move
   * @throws IllegalArgumentException if the CWD could not be changed
   */
  protected void changeWorkingDirectory(String path) {
    if (!tryChangeWorkingDirectory(path)) {
      throw new IllegalArgumentException(format("Could not change working directory to '%s'. Path doesn't exists or is not a directory",
                                                path.toString()));
    }
    LOGGER.debug("working directory changed to {}", path);
  }

  /**
   * Attempts to change the current working directory. If it was not possible (for example, because it doesn't exists), it returns
   * {@code false}
   *
   * @param path the path to which you wish to move
   * @return {@code true} if the CWD was changed. {@code false} otherwise
   */
  protected abstract boolean tryChangeWorkingDirectory(String path);

  /**
   * Template method that renames the file at {@code filePath} to {@code newName}.
   * <p>
   * This method performs path resolution and validation and eventually delegates into {@link #doRename(String, String)}, in which
   * the actual renaming implementation is.
   *
   * @param config the config that is parameterizing this operation
   * @param filePath the path of the file to be renamed
   * @param newName the new name
   * @param overwrite whether to overwrite the target file if it already exists
   */
  protected void rename(FileConnectorConfig config, String filePath, String newName, boolean overwrite) {
    Path source = resolveExistingPath(config, filePath);
    Path target = source.getParent().resolve(newName);

    if (exists(config, target)) {
      if (!overwrite) {
        throw new IllegalArgumentException(format("'%s' cannot be renamed because '%s' already exists", source, target));
      }

      try {
        fileSystem.delete(config, target.toString());
      } catch (Exception e) {
        throw exception(format("Exception was found deleting '%s' as part of renaming '%s'", target, source), e);
      }
    }

    try {
      doRename(source.toString(), target.toString());
      LOGGER.debug("{} renamed to {}", filePath, newName);
    } catch (Exception e) {
      throw exception(format("Exception was found renaming '%s' to '%s'", source, newName), e);
    }
  }

  /**
   * Template method which works in tandem with {@link #rename(FileConnectorConfig, String, String, boolean)}.
   * <p>
   * Implementations are to perform the actual renaming logic here
   *
   * @param filePath the path of the file to be renamed
   * @param newName the new name
   * @throws Exception if anything goes wrong
   */
  protected abstract void doRename(String filePath, String newName) throws Exception;

  protected void createDirectory(FileConnectorConfig config, String directoryPath) {
    final Path path = Paths.get(config.getWorkingDir()).resolve(directoryPath);
    FileAttributes targetFile = getFile(config, directoryPath);

    if (targetFile != null) {
      throw new IllegalArgumentException(format("Directory '%s' already exists", directoryPath));
    }

    mkdirs(config, path);
  }

  /**
   * Performs the base logic and delegates into
   * {@link AbstractFtpCopyDelegate#doCopy(FileConnectorConfig, FileAttributes, Path, boolean, MuleEvent)} to perform the actual
   * copying logic
   *
   * @param config the config that is parameterizing this operation
   * @param sourcePath the path to be copied
   * @param target the path to the target destination
   * @param overwrite whether to overwrite existing target paths
   * @param createParentDirectory whether to create the target's parent directory if it doesn't exists
   * @param event the {@link MuleEvent} which triggered this operation
   */
  protected final void copy(FileConnectorConfig config, String sourcePath, String target, boolean overwrite,
                            boolean createParentDirectory, MuleEvent event, FtpCopyDelegate delegate) {
    FileAttributes sourceFile = getExistingFile(config, sourcePath);
    Path targetPath = resolvePath(config, target);
    FileAttributes targetFile = getFile(config, targetPath.toString());

    if (targetFile != null) {
      if (targetFile.isDirectory()) {
        if (sourceFile.isDirectory() && sourceFile.getName().equals(targetFile.getName()) && !overwrite) {
          throw alreadyExistsException(targetPath);
        } else {
          targetPath = targetPath.resolve(sourceFile.getName());
        }
      } else if (!overwrite) {
        throw alreadyExistsException(targetPath);
      }
    } else {
      if (createParentDirectory) {
        mkdirs(config, targetPath);
        targetPath = targetPath.resolve(sourceFile.getName());
      } else {
        throw new IllegalArgumentException(String
            .format("Can't copy '%s' to '%s' because the destination path " + "doesn't exists", sourceFile.getPath(),
                    targetPath.toAbsolutePath()));
      }
    }

    final String cwd = getCurrentWorkingDirectory();
    delegate.doCopy(config, sourceFile, targetPath, overwrite, event);
    LOGGER.debug("Copied '{}' to '{}'", sourceFile, targetPath);
    changeWorkingDirectory(cwd);
  }

  /**
   * @return the path of the current working directory
   */
  protected abstract String getCurrentWorkingDirectory();
}
