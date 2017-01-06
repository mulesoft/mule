/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import static java.lang.String.format;
import org.mule.extension.file.api.LocalFileAttributes;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.ListCommand;
import org.mule.extension.file.common.api.exceptions.FileAccessDeniedException;
import org.mule.extension.file.internal.LocalFileSystem;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A {@link LocalFileCommand} which implements the {@link ListCommand}
 *
 * @since 4.0
 */
public final class LocalListCommand extends LocalFileCommand implements ListCommand {

  /**
   * {@inheritDoc}
   */
  public LocalListCommand(LocalFileSystem fileSystem) {
    super(fileSystem);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Result<InputStream, FileAttributes>> list(FileConnectorConfig config,
                                                        String directoryPath,
                                                        boolean recursive,
                                                        MediaType mediaType,
                                                        Predicate<FileAttributes> matcher) {
    Path path = resolveExistingPath(directoryPath);
    if (!Files.isDirectory(path)) {
      throw cannotListFileException(path);
    }

    List<Result<InputStream, FileAttributes>> accumulator = new LinkedList<>();
    doList(config, path.toFile(), accumulator, recursive, mediaType, matcher);

    return accumulator;
  }

  private void doList(FileConnectorConfig config,
                      File parent,
                      List<Result<InputStream, FileAttributes>> accumulator,
                      boolean recursive,
                      MediaType mediaType,
                      Predicate<FileAttributes> matcher) {

    if (!parent.canRead()) {
      throw new FileAccessDeniedException(
                                          format("Could not list files from directory '%s' because access was denied by the operating system",
                                                 parent.getAbsolutePath()));
    }

    for (File child : parent.listFiles()) {
      Path path = child.toPath();
      FileAttributes attributes = new LocalFileAttributes(path);
      if (!matcher.test(attributes)) {
        continue;
      }

      if (child.isDirectory()) {
        accumulator.add(Result.<InputStream, FileAttributes>builder().output(null).attributes(attributes).build());

        if (recursive) {
          doList(config, child, accumulator, recursive, mediaType, matcher);
        }
      } else {
        accumulator.add(fileSystem.read(config, child.getAbsolutePath(), mediaType, false));
      }
    }
  }
}
