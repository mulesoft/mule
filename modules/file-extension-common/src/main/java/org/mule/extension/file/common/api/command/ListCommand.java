/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api.command;

import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;

/**
 * Command design pattern for listing files
 *
 * @since 4.0
 */
public interface ListCommand {

  /**
   * Lists files under the considerations of {@link FileSystem#list(FileConnectorConfig, String, boolean, MediaType, Predicate)}
   *
   * @param config        the config that is parameterizing this operation
   * @param directoryPath the path to the directory to be listed
   * @param recursive     whether to include the contents of sub-directories
   * @param matcher       a {@link Predicate} of {@link FileAttributes} used to filter the output list
   * @return a {@link List} of {@link Result} objects each one containing each file's content in the payload and metadata in the attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exists or is not a directory
   * @oaram mediaType the {@link MediaType} of the message which entered the operation
   */
  List<Result<InputStream, FileAttributes>> list(FileConnectorConfig config,
                                                 String directoryPath,
                                                 boolean recursive,
                                                 MediaType mediaType,
                                                 Predicate<FileAttributes> matcher);
}
