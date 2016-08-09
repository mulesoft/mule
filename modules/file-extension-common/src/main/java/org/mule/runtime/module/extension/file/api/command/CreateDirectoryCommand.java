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
 * Command design pattern for creating directories
 *
 * @since 4.0
 */
public interface CreateDirectoryCommand {

  /**
   * Creates a directory under the considerations of {@link FileSystem#createDirectory(FileConnectorConfig, String)}
   *
   * @param config the config that is parameterizing this operation
   * @param directoryName the new directory's new name
   */
  void createDirectory(FileConnectorConfig config, String directoryName);
}
