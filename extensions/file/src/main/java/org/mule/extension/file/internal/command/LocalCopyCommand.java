/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.CopyCommand;
import org.mule.extension.file.internal.LocalFileSystem;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.FileUtils;

import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link AbstractLocalCopyCommand} which implements the {@link CopyCommand} contract
 *
 * @since 4.0
 */
public final class LocalCopyCommand extends AbstractLocalCopyCommand implements CopyCommand {

  /**
   * {@inheritDoc}
   */
  public LocalCopyCommand(LocalFileSystem fileSystem) {
    super(fileSystem);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void copy(FileConnectorConfig config, String sourcePath, String targetDirectory, boolean overwrite,
                   boolean createParentDirectories, Event event) {
    execute(sourcePath, targetDirectory, overwrite, createParentDirectories);
  }

  /**
   * Implements recursive copy
   *
   * @param source the path to be copied
   * @param targetPath the path to the target destination
   * @param overwrite whether to overwrite existing target paths
   * @param options an array of {@link CopyOption} which configure the copying operation
   */
  @Override
  protected void doExecute(Path source, Path targetPath, boolean overwrite, CopyOption[] options) throws Exception {
    if (Files.isDirectory(source)) {
      FileUtils.copyDirectory(source.toFile(), targetPath.toFile());
    } else {
      Files.copy(source, targetPath, options);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getAction() {
    return "copying";
  }
}
