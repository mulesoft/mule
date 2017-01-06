/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import org.mule.extension.file.api.LocalFileAttributes;
import org.mule.extension.file.common.api.BaseFileSystemOperations;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FilePredicateBuilder;
import org.mule.extension.file.common.api.FileSystem;
import org.mule.extension.file.common.api.exceptions.FileListErrorTypeProvider;
import org.mule.extension.file.common.api.exceptions.FileReadErrorTypeProvider;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.annotation.DataTypeParameters;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

/**
 * File connector operations.
 *
 * @since 4.0
 */
public final class FileOperations extends BaseFileSystemOperations {

  /**
   * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
   * <p>
   * If the listing encounters a directory, the output list will include its contents depending on the value of the
   * {@code recursive} parameter.
   * <p>
   * If {@code recursive} is set to {@code true} but a found directory is rejected by the {@code matcher}, then there won't be any
   * recursion into such directory.
   *
   * @param config        the config that is parameterizing this operation
   * @param directoryPath the path to the directory to be listed
   * @param recursive     whether to include the contents of sub-directories. Defaults to false.
   * @param matchWith     a matcher used to filter the output list
   * @return a {@link List} of {@link Message messages} each one containing each file's content in the payload and metadata in the attributes
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exists or is not a directory
   * @oaram mediaType the {@link MediaType} of the message which entered the operation
   */
  @Summary("List all the files from given directory")
  @Throws(FileListErrorTypeProvider.class)
  public List<Result<InputStream, LocalFileAttributes>> list(@UseConfig FileConnectorConfig config,
                                                             @Connection LocalFileSystem fileSystem,
                                                             @Optional String directoryPath,
                                                             @Optional(defaultValue = "false") boolean recursive,
                                                             MediaType mediaType,
                                                             @Optional @DisplayName("File Matching Rules") @Summary("Matcher to filter the listed files") FilePredicateBuilder matchWith) {
    List result = doList(config, fileSystem, directoryPath, recursive, mediaType, matchWith);
    return (List<Result<InputStream, LocalFileAttributes>>) result;
  }

  /**
   * Obtains the content and metadata of a file at a given path. The operation itself returns a {@link Message} which payload is a
   * {@link InputStream} with the file's content, and the metadata is represent as a {@link LocalFileAttributes} object that's placed
   * as the message {@link Message#getAttributes() attributes}.
   * <p>
   * If the {@code lock} parameter is set to {@code true}, then a file system level lock will be placed on the file until the
   * input stream this operation returns is closed or fully consumed. Because the lock is actually provided by the host file
   * system, its behavior might change depending on the mounted drive and the operation system on which mule is running. Take that
   * into consideration before blindly relying on this lock.
   * <p>
   * This method also makes a best effort to determine the mime type of the file being read. A {@link MimetypesFileTypeMap} will
   * be used to make an educated guess on the file's mime type. The user also has the chance to force the output encoding and
   * mimeType through the {@code outputEncoding} and {@code outputMimeType} optional parameters.
   *
   * @param config     the config that is parameterizing this operation
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param path       the path to the file to be read
   * @param lock       whether or not to lock the file. Defaults to false.
   * @return the file's content and metadata on a {@link FileAttributes} instance
   * @throws IllegalArgumentException if the file at the given path doesn't exists
   * @oaram mediaType the {@link MediaType} of the message which entered the operation
   */
  @DataTypeParameters
  @Summary("Obtains the content and metadata of a file at a given path")
  @Throws(FileReadErrorTypeProvider.class)
  public Result<InputStream, LocalFileAttributes> read(@UseConfig FileConnectorConfig config,
                                                       @Connection FileSystem fileSystem,
                                                       @DisplayName("File Path") String path,
                                                       MediaType mediaType,
                                                       @Optional(defaultValue = "false") boolean lock) {
    Result result = doRead(config, fileSystem, path, mediaType, lock);
    return (Result<InputStream, LocalFileAttributes>) result;
  }

}
