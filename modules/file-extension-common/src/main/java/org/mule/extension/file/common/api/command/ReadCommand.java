/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.command;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;

import java.io.InputStream;

/**
 * Command design pattern for reading files
 *
 * @since 4.0
 */
public interface ReadCommand {

  /**
   * Reads files under the considerations of {@link FileSystem#read(FileConnectorConfig, Message, String, boolean)}
   *
   * @param config the config that is parameterizing this operation
   * @param message the incoming Message
   * @param filePath the path of the file you want to read
   * @param lock whether or not to lock the file
   * @return An {@link Result} with an {@link InputStream} with the file's content as payload and a
   *         {@link FileAttributes} object as {@link Message#getAttributes()}
   * @throws IllegalArgumentException if the file at the given path doesn't exists
   */
  Result<InputStream, FileAttributes> read(FileConnectorConfig config, Message message, String filePath,
                                           boolean lock);
}
