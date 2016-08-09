/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.walkFileTree;
import org.mule.extension.file.internal.LocalFileSystem;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.DeleteCommand;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LocalFileCommand} which implements the {@link DeleteCommand} contract
 *
 * @since 4.0
 */
public final class LocalDeleteCommand extends LocalFileCommand implements DeleteCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalDeleteCommand.class);

  /**
   * {@inheritDoc}
   */
  public LocalDeleteCommand(LocalFileSystem fileSystem) {
    super(fileSystem);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(FileConnectorConfig config, String filePath) {
    Path path = resolveExistingPath(config, filePath);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Preparing to delete '{}'", path);
    }

    try {
      if (isDirectory(path)) {
        walkFileTree(path, new SimpleFileVisitor<Path>() {

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            doDelete(file);
            return CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (super.postVisitDirectory(dir, exc) == CONTINUE) {
              doDelete(dir);
            }

            return CONTINUE;
          }
        });
      } else {
        doDelete(path);
      }
    } catch (AccessDeniedException e) {
      throw exception(format("Could not delete file '%s' because access was denied by the operating system", path), e);
    } catch (IOException e) {
      throw exception(format("Could not delete '%s'", path), e);
    }
  }

  private void doDelete(Path path) throws IOException {
    Files.delete(path);
    logDeletion(path);
  }

  private void logDeletion(Path path) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Successfully deleted '{}'", path);
    }
  }
}
