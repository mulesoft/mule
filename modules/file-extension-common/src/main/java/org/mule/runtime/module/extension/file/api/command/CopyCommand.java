/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api.command;

import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.FileSystem;

/**
 * Command for copying files
 *
 * @since 4.0
 */
public interface CopyCommand {

  /**
   * Performs a copy operation under the considerations of
   * {@link FileSystem#copy(FileConnectorConfig, String, String, boolean, boolean, MuleEvent)}
   *
   * @param config the config that is parameterizing this operation
   * @param sourcePath the path to the file to be copied
   * @param targetPath the target directory
   * @param overwrite whether or not overwrite the file if the target destination already exists.
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param event whether or not to attempt creating the parent directory if it doesn't exists.
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  void copy(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite, boolean createParentDirectories,
            MuleEvent event);
}
