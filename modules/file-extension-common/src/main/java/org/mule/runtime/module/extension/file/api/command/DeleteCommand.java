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
 * Command design pattern for deleting files
 *
 * @since 4.0
 */
public interface DeleteCommand {

  /**
   * Deletes a file under the considerations of {@link FileSystem#delete(FileConnectorConfig, String)}
   *
   * @param config the config that is parameterizing this operation
   * @param filePath the path to the file to be deleted
   * @throws IllegalArgumentException if {@code filePath} doesn't exists or is locked
   */
  void delete(FileConnectorConfig config, String filePath);
}
