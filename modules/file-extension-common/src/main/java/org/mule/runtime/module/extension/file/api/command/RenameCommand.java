/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api.command;

import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.FileSystem;

/**
 * Command design pattern for reading files
 *
 * @since 4.0
 */
public interface RenameCommand {

  /**
   * Renames a file under the considerations of {@link FileSystem#rename(FileConnectorConfig, String, String, boolean)}
   *
   * @param config the config that is parameterizing this operation
   * @param filePath the path to the file to be renamed
   * @param newName the file's new name
   * @param overwrite whether to overwrite the target file if it already exists
   */
  void rename(FileConnectorConfig config, String filePath, String newName, boolean overwrite);
}
