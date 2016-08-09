/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import static java.lang.String.format;
import org.mule.extension.file.api.LocalFileAttributes;
import org.mule.extension.file.internal.LocalFileSystem;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.TreeNode;
import org.mule.runtime.module.extension.file.api.command.ListCommand;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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
  public TreeNode list(FileConnectorConfig config, String directoryPath, boolean recursive, MuleMessage message,
                       Predicate<FileAttributes> matcher) {
    Path path = resolveExistingPath(config, directoryPath);
    if (!Files.isDirectory(path)) {
      throw cannotListFileException(path);
    }

    TreeNode.Builder treeNodeBuilder = TreeNode.Builder.forDirectory(new LocalFileAttributes(path));
    doList(config, path.toFile(), treeNodeBuilder, recursive, message, matcher);

    return treeNodeBuilder.build();
  }

  private void doList(FileConnectorConfig config, File parent, TreeNode.Builder treeNodeBuilder, boolean recursive,
                      MuleMessage message, Predicate<FileAttributes> matcher) {
    if (!parent.canRead()) {
      throw exception(format("Could not list files from directory '%s' because access was denied by the operating system",
                             parent.getAbsolutePath()));
    }

    for (File child : parent.listFiles()) {
      Path path = child.toPath();
      FileAttributes attributes = new LocalFileAttributes(path);
      if (!matcher.test(attributes)) {
        continue;
      }

      if (child.isDirectory()) {
        TreeNode.Builder childNodeBuilder = TreeNode.Builder.forDirectory(attributes);
        treeNodeBuilder.addChild(childNodeBuilder);

        if (recursive) {
          doList(config, child, childNodeBuilder, recursive, message, matcher);
        }
      } else {
        treeNodeBuilder.addChild(TreeNode.Builder.forFile(fileSystem.read(config, message, child.getAbsolutePath(), false)));
      }
    }
  }
}
