/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.ListCommand;
import org.mule.extension.ftp.api.ftp.ClassicFtpFileAttributes;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;

/**
 * A {@link ClassicFtpCommand} which implements the {@link ListCommand} contract
 *
 * @since 4.0
 */
public final class FtpListCommand extends ClassicFtpCommand implements ListCommand {

  private static final Logger LOGGER = getLogger(FtpListCommand.class);
  private static final int FTP_LIST_PAGE_SIZE = 25;

  /**
   * {@inheritDoc}
   */
  public FtpListCommand(ClassicFtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
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

    FileAttributes directoryAttributes = getExistingFile(directoryPath);
    Path path = Paths.get(directoryAttributes.getPath());

    if (!directoryAttributes.isDirectory()) {
      throw cannotListFileException(path);
    }

    if (!tryChangeWorkingDirectory(path.toString())) {
      throw exception(format("Could not change working directory to '%s' while trying to list that directory", path));
    }

    List<Result<InputStream, FileAttributes>> accumulator = new LinkedList<>();

    try {
      doList(config, path, accumulator, recursive, mediaType, matcher);

      if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
        throw exception(format("Failed to list files on directory '%s'", path));
      }

      changeWorkingDirectory(path);
    } catch (Exception e) {
      throw exception(format("Failed to list files on directory '%s'", path), e);
    }

    return accumulator;
  }

  private void doList(FileConnectorConfig config,
                      Path path,
                      List<Result<InputStream, FileAttributes>> accumulator,
                      boolean recursive,
                      MediaType mediaType,
                      Predicate<FileAttributes> matcher)
      throws IOException {
    LOGGER.debug("Listing directory {}", path);

    FTPListParseEngine engine = client.initiateListParsing();
    while (engine.hasNext()) {
      FTPFile[] files = engine.getNext(FTP_LIST_PAGE_SIZE);
      if (ArrayUtils.isEmpty(files)) {
        return;
      }

      for (FTPFile file : files) {
        final Path filePath = path.resolve(file.getName());
        FileAttributes attributes = new ClassicFtpFileAttributes(filePath, file);

        if (isVirtualDirectory(attributes.getName()) || !matcher.test(attributes)) {
          continue;
        }

        if (attributes.isDirectory()) {
          accumulator.add(Result.<InputStream, FileAttributes>builder().output(null).attributes(attributes).build());

          if (recursive) {
            Path recursionPath = path.resolve(attributes.getName());
            if (!client.changeWorkingDirectory(attributes.getName())) {
              throw exception(format("Could not change working directory to '%s' while performing recursion on list operation",
                                     recursionPath));
            }
            doList(config, recursionPath, accumulator, recursive, mediaType, matcher);
            if (!client.changeToParentDirectory()) {
              throw exception(format("Could not return to parent working directory '%s' while performing recursion on list operation",
                                     recursionPath.getParent()));
            }
          }
        } else {
          accumulator.add(fileSystem.read(config, filePath.toString(), mediaType, false));
        }
      }
    }
  }
}
