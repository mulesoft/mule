/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.common.api;

import static java.lang.String.format;
import static java.nio.file.Paths.get;
import org.mule.extension.file.common.api.exceptions.FileCopyErrorTypeProvider;
import org.mule.extension.file.common.api.exceptions.FileDeleteErrorTypeProvider;
import org.mule.extension.file.common.api.exceptions.FileListErrorTypeProvider;
import org.mule.extension.file.common.api.exceptions.FileReadErrorTypeProvider;
import org.mule.extension.file.common.api.exceptions.FileRenameErrorTypeProvider;
import org.mule.extension.file.common.api.exceptions.FileWriteErrorTypeProvider;
import org.mule.extension.file.common.api.exceptions.IllegalContentException;
import org.mule.extension.file.common.api.exceptions.IllegalPathException;
import org.mule.extension.file.common.api.matcher.NullFilePayloadPredicate;
import org.mule.extension.file.common.api.metadata.FileTreeNodeMetadataResolver;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.annotation.DataTypeParameters;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Predicate;

import javax.activation.MimetypesFileTypeMap;

/**
 * Basic set of operations for extensions which perform operations over a generic file system
 *
 * @since 4.0
 */
// TODO: MULE-9215
public class StandardFileSystemOperations {

  /**
   * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
   * <p>
   * If the listing encounters a directory, the output list will include its contents depending on the value of the
   * {@code recursive} parameter.
   * <p>
   * If {@code recursive} is set to {@code true} but a found directory is rejected by the {@code matcher}, then there won't be any
   * recursion into such directory.
   *
   * @param config the config that is parameterizing this operation
   * @param directoryPath the path to the directory to be listed
   * @param recursive whether to include the contents of sub-directories. Defaults to false.
   * @param message the {@link Message} on which this operation was triggered
   * @param matchWith a matcher used to filter the output list
   * @return a {@link TreeNode} object representing the listed directory
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exists or is not a directory
   */
  @Summary("List all the files from given directory")
  @OutputResolver(output = FileTreeNodeMetadataResolver.class)
  @Throws(FileListErrorTypeProvider.class)
  public TreeNode list(@UseConfig FileConnectorConfig config, @Connection FileSystem fileSystem, @Optional String directoryPath,
                       @Optional(defaultValue = "false") boolean recursive, Message message,
                       @Optional @Summary("Matcher to filter the listed files") FilePredicateBuilder matchWith) {
    fileSystem.changeToBaseDir();
    return fileSystem.list(config, directoryPath, recursive, message, getPredicate(matchWith));
  }

  /**
   * Obtains the content and metadata of a file at a given path. The operation itself returns a {@link Message} which payload is a
   * {@link InputStream} with the file's content, and the metadata is represent as a {@link FileAttributes} object that's placed
   * as the message {@link Message#getAttributes() attributes}.
   * <p>
   * If the {@code lock} parameter is set to {@code true}, then a file system level lock will be placed on the file until the
   * input stream this operation returns is closed or fully consumed. Because the lock is actually provided by the host file
   * system, its behavior might change depending on the mounted drive and the operation system on which mule is running. Take that
   * into consideration before blindly relying on this lock.
   * <p>
   * This method also makes a best effort to determine the mime type of the file being read. A {@link MimetypesFileTypeMap} will
   * be used to make an educated guess on the file's mime type. The user also has the chance to force the output enconding and
   * mimeType through the {@code outputEncoding} and {@code outputMimeType} optional parameters.
   *
   * @param config the config that is parameterizing this operation
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param message the incoming {@link Message}
   * @param path the path to the file to be read
   * @param lock whether or not to lock the file. Defaults to false.
   * @return the file's content and metadata on a {@link FileAttributes} instance
   * @throws IllegalArgumentException if the file at the given path doesn't exists
   */
  @DataTypeParameters
  @Summary("Obtains the content and metadata of a file at a given path")
  @Throws(FileReadErrorTypeProvider.class)
  public Result<InputStream, FileAttributes> read(@UseConfig FileConnectorConfig config,
                                                  @Connection FileSystem fileSystem, Message message,
                                                  @DisplayName("File Path") String path,
                                                  @Optional(defaultValue = "false") boolean lock) {
    fileSystem.changeToBaseDir();
    return fileSystem.read(config, message, path, lock);
  }

  /**
   * Writes the {@code content} into the file pointed by {@code path}.
   * <p>
   * The {@code content} can be of any of the given types:
   * <ul>
   * <li>{@link String}</li>
   * <li>{@code String[]}</li>
   * <li>{@code byte}</li>
   * <li>{@code byte[]}</li>
   * <li>{@link OutputHandler}</li>
   * <li>{@link Iterable}</li>
   * <li>{@link Iterator}</li>
   * </ul>
   * <p>
   * {@code null} contents are not allowed and will result in an {@link IllegalArgumentException}.
   * <p>
   * To support pass-through scenarios, the {@code path} attribute is optional. If not provided, then the current
   * {@link Message#getAttributes()} value will be tested to be an instance of {@link FileAttributes}, in which case
   * {@link FileAttributes#getPath()} will be used. If that's not the case, then an {@link IllegalArgumentException} will be
   * thrown.
   * <p>
   * If the directory on which the file is attempting to be written doesn't exist, then the operation will either throw
   * {@link IllegalArgumentException} or create such folder depending on the value of the {@code createParentDirectory}.
   * <p>
   * If the file itself already exists, then the behavior depends on the supplied {@code mode}.
   * <p>
   * This operation also supports locking support depending on the value of the {@code lock} argument, but following the same
   * rules and considerations as described in the read operation.
   *
   * @param config the {@link FileConnectorConfig} on which the operation is being executed
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param path the path of the file to be written
   * @param content the content to be written into the file. Defaults to the current {@link Message} payload
   * @param mode a {@link FileWriteMode}. Defaults to {@code OVERWRITE}
   * @param lock whether or not to lock the file. Defaults to false
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param encoding when {@code content} is a {@link String}, this attribute specifies the encoding to be used when writing. If
   *        not set, then it defaults to {@link FileConnectorConfig#getDefaultWriteEncoding()}
   * @param event The current {@link Event}
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  @Summary("Writes the given \"Content\" in the file pointed by \"Path\"")
  @Throws(FileWriteErrorTypeProvider.class)
  public void write(@UseConfig FileConnectorConfig config, @Connection FileSystem fileSystem, @Optional String path,
                    @Content @Summary("Content to be written into the file") Object content,
                    @Optional(
                        defaultValue = "OVERWRITE") @Summary("How the file is going to be written") @DisplayName("Write Mode") FileWriteMode mode,
                    @Optional(defaultValue = "false") boolean lock,
                    @Optional(defaultValue = "true") boolean createParentDirectories,
                    @Optional @Summary("Encoding when trying to write a String file. If not set, defaults to the configuration one or the Mule default") String encoding,
                    Event event) {
    if (content == null) {
      throw new IllegalContentException("Cannot write a null content");
    }

    fileSystem.changeToBaseDir();
    path = resolvePath(path, event, "path");

    if (encoding == null) {
      encoding = config.getDefaultWriteEncoding();
    }

    fileSystem.write(path, content, mode, event, lock, createParentDirectories, encoding);
  }

  /**
   * Copies the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * To support pass-through scenarios, the {@code sourcePath} attribute is optional. If not provided, then the current
   * {@link Message#getAttributes()} value will be tested to be an instance of {@link FileAttributes}, in which case
   * {@link FileAttributes#getPath()} will be used. If that's not the case, then an {@link IllegalArgumentException} will be
   * thrown.
   * <p>
   * If {@code targetPath} doesn't exists, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentFolder} argument. If such argument is {@false}, then an {@link IllegalArgumentException} will
   * be thrown.
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@link IllegalArgumentException} will be thrown.
   * <p>
   * It is also possible to use the {@code targetPath} to specify that the copied file should also be renamed. For example, if
   * {@code sourcePath} has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to <i>a/c/test.json</i>, then the
   * file will indeed be copied to the <i>a/c/</i> directory but renamed as <i>test.json</i>
   * <p>
   * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be copied
   * recursively.
   *
   * @param config the config that is parameterizing this operation
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param sourcePath the path to the file to be copied
   * @param targetPath the target directory where the file is going to be copied
   * @param overwrite whether or not overwrite the file if the target destination already exists.
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param event the {@link Event} which triggered this operation
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  @Summary("Copies a file in another directory")
  @Throws(FileCopyErrorTypeProvider.class)
  public void copy(@UseConfig FileConnectorConfig config, @Connection FileSystem fileSystem, @Optional String sourcePath,
                   String targetPath, @Optional(defaultValue = "false") boolean overwrite,
                   @Optional(defaultValue = "true") boolean createParentDirectories, Event event) {
    fileSystem.changeToBaseDir();
    validateTargetPath(targetPath);
    sourcePath = resolvePath(sourcePath, event, "sourcePath");
    fileSystem.copy(config, sourcePath, targetPath, overwrite, createParentDirectories, event);
  }

  private void validateTargetPath(String targetPath) {
    if (StringUtils.isBlank(targetPath)) {
      throw new IllegalPathException("target path cannot be null nor blank");
    }
  }

  /**
   * Moves the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * To support pass-through scenarios, the {@code sourcePath} attribute is optional. If not provided, then the current
   * {@link Message#getAttributes()} value will be tested to be an instance of {@link FileAttributes}, in which case
   * {@link FileAttributes#getPath()} will be used. If that's not the case, then an {@link IllegalArgumentException} will be
   * thrown.
   * <p>
   * If {@code targetPath} doesn't exists, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentFolder} argument. If such argument is {@code false}, then an {@link IllegalArgumentException}
   * will be thrown.
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@link IllegalArgumentException} will be thrown.
   * <p>
   * It is also possible to use the {@code targetPath} to specify that the moved file should also be renamed. For example, if
   * {@code sourcePath} has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to <i>a/c/test.json</i>, then the
   * file will indeed be copied to the <i>a/c/</i> directory but renamed as <i>test.json</i>
   * <p>
   * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be moved
   * recursively.
   *
   * @param config the config that is parameterizing this operation
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param sourcePath the path to the file to be copied
   * @param targetPath the target directory
   * @param overwrite whether or not overwrite the file if the target destination already exists.
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param event The current {@link Event}
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  @Summary("Moves a file to another directory")
  @Throws(FileCopyErrorTypeProvider.class)
  public void move(@UseConfig FileConnectorConfig config, @Connection FileSystem fileSystem, @Optional String sourcePath,
                   String targetPath, @Optional(defaultValue = "false") boolean overwrite,
                   @Optional(defaultValue = "true") boolean createParentDirectories, Event event) {
    fileSystem.changeToBaseDir();
    validateTargetPath(targetPath);
    sourcePath = resolvePath(sourcePath, event, "sourcePath");
    fileSystem.move(config, sourcePath, targetPath, overwrite, createParentDirectories);
  }

  /**
   * Deletes the file pointed by {@code path}, provided that it's not locked
   * <p>
   * To support pass-through scenarios, the {@code path} attribute is optional. If not provided, then the current
   * {@link Message#getAttributes()} value will be tested to be an instance of {@link FileAttributes}, in which case
   * {@link FileAttributes#getPath()} will be used. If that's not the case, then an {@link IllegalArgumentException} will be
   * thrown.
   *
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param path the path to the file to be deleted
   * @param event The current {@link Event}
   * @throws IllegalArgumentException if {@code filePath} doesn't exists or is locked
   */
  @Summary("Deletes a file")
  @Throws(FileDeleteErrorTypeProvider.class)
  public void delete(@Connection FileSystem fileSystem, @Optional String path, Event event) {
    fileSystem.changeToBaseDir();
    path = resolvePath(path, event, "path");
    fileSystem.delete(path);
  }

  /**
   * Renames the file pointed by {@code path} to the name provided on the {@code to} parameter
   * <p>
   * To support pass-through scenarios, the {@code path} attribute is optional. If not provided, then the current
   * {@link Message#getAttributes()} value will be tested to be an instance of {@link FileAttributes}, in which case
   * {@link FileAttributes#getPath()} will be used. If that's not the case, then an {@link IllegalArgumentException} will be
   * thrown.
   * <p>
   * {@code to} argument should not contain any path separator. {@link IllegalArgumentException} will be thrown if this
   * precondition is not honored.
   *
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param path the path to the file to be renamed
   * @param to the file's new name
   * @param overwrite whether or not overwrite the file if the target destination already exists.
   * @param event The current {@link Event}
   */
  // TODO: MULE-9715
  @Summary("Renames a file")
  @Throws(FileRenameErrorTypeProvider.class)
  public void rename(@Connection FileSystem fileSystem, @Optional String path,
                     @DisplayName("New Name") String to, @Optional(defaultValue = "false") boolean overwrite, Event event) {
    if (get(to).getNameCount() != 1) {
      throw new IllegalPathException(format("'to' parameter of rename operation should not contain any file separator character but '%s' was received",
                                            to));
    }

    fileSystem.changeToBaseDir();
    path = resolvePath(path, event, "path");
    fileSystem.rename(path, to, overwrite);
  }

  /**
   * Creates a new directory on {@code directoryPath}
   *
   * @param fileSystem a reference to the host {@link FileSystem}
   * @param directoryPath the new directory's name
   */
  @Summary("Creates a new directory")
  @Throws(FileRenameErrorTypeProvider.class)
  public void createDirectory(@Connection FileSystem fileSystem, String directoryPath) {
    fileSystem.changeToBaseDir();
    fileSystem.createDirectory(directoryPath);
  }

  private String resolvePath(String path, Event event, String attributeName) {
    if (!StringUtils.isBlank(path)) {
      return path;
    }

    Message message = event.getMessage();
    if (message.getAttributes() instanceof FileAttributes) {
      return ((FileAttributes) message.getAttributes()).getPath();
    }

    throw new IllegalPathException(format("A %s was not specified and a default one could not be obtained from the current message attributes",
                                          attributeName));
  }

  private Predicate<FileAttributes> getPredicate(FilePredicateBuilder builder) {
    return builder != null ? builder.build() : new NullFilePayloadPredicate();
  }
}
