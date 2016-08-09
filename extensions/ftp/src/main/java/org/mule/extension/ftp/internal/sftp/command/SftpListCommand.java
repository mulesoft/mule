/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.command;

import static org.mule.runtime.module.extension.file.api.TreeNode.Builder.forDirectory;
import static org.mule.runtime.module.extension.file.api.TreeNode.Builder.forFile;

import org.mule.extension.ftp.api.sftp.SftpFileAttributes;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.extension.ftp.internal.sftp.connection.SftpFileSystem;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.TreeNode;
import org.mule.runtime.module.extension.file.api.command.ListCommand;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SftpCommand} which implements the {@link ListCommand} contract
 *
 * @since 4.0
 */
public final class SftpListCommand extends SftpCommand implements ListCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpListCommand.class);

  /**
   * {@inheritDoc}
   */
  public SftpListCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeNode list(FileConnectorConfig config, String directoryPath, boolean recursive, MuleMessage message,
                       Predicate<FileAttributes> matcher) {
    FileAttributes directoryAttributes = getExistingFile(config, directoryPath);
    Path path = Paths.get(directoryAttributes.getPath());

    if (!directoryAttributes.isDirectory()) {
      throw cannotListFileException(path);
    }

    TreeNode.Builder treeNodeBuilder = forDirectory(directoryAttributes);
    doList(config, directoryAttributes.getPath(), treeNodeBuilder, recursive, message, matcher);

    return treeNodeBuilder.build();
  }

  private void doList(FileConnectorConfig config, String path, TreeNode.Builder treeNodeBuilder, boolean recursive,
                      MuleMessage message, Predicate<FileAttributes> matcher) {
    LOGGER.debug("Listing directory {}", path);
    for (SftpFileAttributes file : client.list(path)) {
      if (isVirtualDirectory(file.getName()) || !matcher.test(file)) {
        continue;
      }

      if (file.isDirectory()) {
        TreeNode.Builder childNodeBuilder = forDirectory(file);
        treeNodeBuilder.addChild(childNodeBuilder);

        if (recursive) {
          doList(config, file.getPath(), childNodeBuilder, recursive, message, matcher);
        }
      } else {
        treeNodeBuilder.addChild(forFile(fileSystem.read(config, message, file.getPath(), false)));
      }
    }
  }
}
