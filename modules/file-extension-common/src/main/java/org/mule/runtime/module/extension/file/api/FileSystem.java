/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.runtime.module.extension.file.api.lock.PathLock;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

import javax.activation.MimetypesFileTypeMap;

/**
 * Represents an abstract file system and the operations which can be performed on it.
 * <p>
 * This interface acts as a facade which allows performing common files operations regardless of those files being in a local
 * disk, an FTP server, a cloud storage service, etc.
 *
 * @since 4.0
 */
public interface FileSystem {

  /**
   * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
   * <p>
   * If the listing encounters a directory, the output list will include its contents depending on the value of the
   * {@code recursive} argument.
   * <p>
   * If {@code recursive} is set to {@code true} but a found directory is rejected by the {@code matcher}, then there won't be any
   * recursion into such directory.
   *
   * @param config the config that is parameterizing this operation
   * @param directoryPath the path to the directory to be listed
   * @param recursive whether to include the contents of sub-directories
   * @param message the {@link MuleMessage} on which this operation was triggered
   * @param matcher a {@link Predicate} of {@link FileAttributes} used to filter the output list
   * @return a {@link TreeNode} object representing the listed directory
   * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exists or is not a directory
   */
  TreeNode list(FileConnectorConfig config, String directoryPath, boolean recursive, MuleMessage message,
                Predicate<FileAttributes> matcher);

  /**
   * Obtains the content and metadata of a file at a given path.
   * <p>
   * Locking can be actually enabled through the {@code lock} argument, however, the extent of such lock will depend on the
   * implementation. What is guaranteed by passing {@code true} on the {@code lock} argument is that {@code this} instance will
   * not attempt to modify this file until the {@link InputStream} returned by {@link OperationResult#getOutput()} this method
   * returns is closed or fully consumed. Some implementation might actually perform a file system level locking which goes beyond
   * the extend of {@code this} instance or even mule. For some other file systems that might be simply not possible and no extra
   * assumptions are to be taken.
   * <p>
   * This method also makes a best effort to determine the mime type of the file being read. a {@link MimetypesFileTypeMap} will
   * be used to make an educated guess on the file's mime type
   *
   * @param config the config that is parameterizing this operation
   * @param message the incoming {@link MuleMessage}
   * @param filePath the path of the file you want to read
   * @param lock whether or not to lock the file
   * @return An {@link OperationResult} with an {@link InputStream} with the file's content as payload and a
   *         {@link FileAttributes} object as {@link MuleMessage#getAttributes()}
   * @throws IllegalArgumentException if the file at the given path doesn't exists
   */
  OperationResult<InputStream, FileAttributes> read(FileConnectorConfig config, MuleMessage message, String filePath,
                                                    boolean lock);

  /**
   * Writes the {@code content} into the file pointed by {@code filePath}.
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
   * If the directory on which the file is attempting to be written doesn't exist, then the operation will either throw
   * {@link IllegalArgumentException} or create such folder depending on the value of the {@code createParentDirectory}.
   * <p>
   * If the file itself already exists, then the behavior depends on the supplied {@code mode}.
   * <p>
   * This method also supports locking support depending on the value of the {@code lock} argument, but following the same rules
   * and considerations as described in the {@link #read(FileConnectorConfig, MuleMessage, String, boolean)} method
   *
   * @param config the config on which is parameterizing this operation
   * @param filePath the path of the file to be written
   * @param content the content to be written into the file
   * @param mode a {@link FileWriteMode}
   * @param event the {@link MuleEvent} which processing triggers this operation
   * @param lock whether or not to lock the file
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @param encoding when {@@code content} is a {@link String}, this attribute specifies the encoding to be used when writing. If
   *        not set, then it defaults to {@link FileConnectorConfig#getDefaultWriteEncoding()}
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  void write(FileConnectorConfig config, String filePath, Object content, FileWriteMode mode, MuleEvent event, boolean lock,
             boolean createParentDirectories, String encoding);

  /**
   * Copies the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * If {@code targetPath} doesn't exists, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentDirectory} argument. If such argument is {@false}, then an {@link IllegalArgumentException}
   * will be thrown.
   * <p>
   * It is also possible to use the {@code targetPath} to specify that the copied file should also be renamed. For example, if
   * {@code sourcePath} has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to <i>a/c/test.json</i>, then the
   * file will indeed be copied to the <i>a/c/</i> directory but renamed as <i>test.json</i>
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@link IllegalArgumentException} will be thrown
   * <p>
   * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be copied
   * recursively
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

  /**
   * Moves the file at the {@code sourcePath} into the {@code targetPath}.
   * <p>
   * If {@code targetPath} doesn't exists, and neither does its parent, then an attempt will be made to create depending on the
   * value of the {@code createParentDirectory} argument. If such argument is {@false}, then an {@link IllegalArgumentException}
   * will be thrown.
   * <p>
   * It is also possible to use the {@code targetPath} to specify that the moved file should also be renamed. For example, if
   * {@code sourcePath} has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to <i>a/c/test.json</i>, then the
   * file will indeed be moved to the <i>a/c/</i> directory but renamed as <i>test.json</i>
   * <p>
   * If the target file already exists, then it will be overwritten if the {@code overwrite} argument is {@code true}. Otherwise,
   * {@link IllegalArgumentException} will be thrown
   * <p>
   * As for the {@code sourcePath}, it can either be a file or a directory. If it points to a directory, then it will be moved
   * recursively
   *
   * @param config the config that is parameterizing this operation
   * @param sourcePath the path to the file to be copied
   * @param targetPath the target directory
   * @param overwrite whether or not overwrite the file if the target destination already exists.
   * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
   * @throws IllegalArgumentException if an illegal combination of arguments is supplied
   */
  void move(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite, boolean createParentDirectories);

  /**
   * Deletes the file pointed by {@code filePath}, provided that it's not locked
   *
   * @param config the config that is parameterizing this operation
   * @param filePath the path to the file to be deleted
   * @throws IllegalArgumentException if {@code filePath} doesn't exists or is locked
   */
  void delete(FileConnectorConfig config, String filePath);

  /**
   * Renames the file pointed by {@code filePath} to the provided {@code newName}
   *
   * @param config the config that is parameterizing this operation
   * @param filePath the path to the file to be renamed
   * @param newName the file's new name
   * @param overwrite whether or not overwrite the file if the target destination already exists.
   */
  void rename(FileConnectorConfig config, String filePath, String newName, boolean overwrite);

  /**
   * Creates a new directory
   *
   * @param config the config that is parameterizing this operation
   * @param directoryPath the new directory's path
   */
  void createDirectory(FileConnectorConfig config, String directoryPath);

  /**
   * Acquires and returns lock over the given {@code path}.
   * <p>
   * Depending on the underlying filesystem, the extent of the lock will depend on the implementation. If a lock can not be
   * acquired, then an {@link IllegalStateException} is thrown.
   * <p>
   * Whoever request the lock <b>MUST</b> release it as soon as possible.
   *
   * @param path the path to the file you want to lock
   * @param params vararg of generic arguments depending on the underlying implementation
   * @return an acquired {@link PathLock}
   * @throws IllegalArgumentException if a lock could not be acquired
   */
  PathLock lock(Path path, Object... params);

  Lock createMuleLock(String id);

  /**
   * Creates a new {@link DataType} to be associated with a {@link MuleMessage} which payload is a {@link InputStream} and the
   * attributes an instance of {@link FileAttributes}
   * <p>
   * It will try to update the {@link DataType#getMediaType()} with a best guess derived from the given {@code attributes}. If no
   * best-guess is possible, then the {@code originalDataType}'s mimeType is honoured.
   * <p>
   * As for the {@link MediaType#getCharset()}, the {@code dataType} one is respected
   *
   * @param originalMediaType the original {@link MediaType} that the {@link MuleMessage} had before executing the operation
   * @param attributes the {@link FileAttributes} of the file being processed
   * @return a {@link DataType} the resulting {@link DataType}.
   */
  MediaType getFileMessageMediaType(MediaType originalMediaType, FileAttributes attributes);

  /**
   * Verify that the given {@code path} is not locked
   *
   * @param path the path to test
   * @throws IllegalStateException if the {@code path} is indeed locked
   */
  void verifyNotLocked(Path path);

  /**
   * Changes the current working directory to the user base
   *
   * @param config the config which is parameterizing this operation
   */
  void changeToBaseDir(FileConnectorConfig config);

  /**
   * The concrete class that represents the attributes related to the {@link FileSystem} implementation.
   * <p>
   * This method is called when handling the dynamic resolution for the output attributes metadata of an operation.
   */
  Class<? extends FileAttributes> getAttributesType();

}
