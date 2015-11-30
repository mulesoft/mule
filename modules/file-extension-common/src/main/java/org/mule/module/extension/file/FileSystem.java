/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file;

import org.mule.api.MuleEvent;
import org.mule.api.transport.OutputHandler;
import org.mule.extension.api.runtime.ContentMetadata;
import org.mule.transport.NullPayload;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import javax.activation.MimetypesFileTypeMap;

/**
 * Represents an abstract file system and the operations which can be performed
 * on it.
 * <p>
 * This interface acts as a facade which allows performing common files operations
 * regardless of those files being in a local disk, an FTP server, a cloud storage
 * service, etc.
 *
 * @since 4.0
 */
public interface FileSystem
{

    /**
     * Lists all the files in the {@code directoryPath} which match the given {@code predicate}.
     * <p>
     * If the listing encounters a directory, the output list will include its contents depending
     * on the value of the {@code recursive} argument.
     * <p>
     * If {@code recursive} is set to {@code true} but a found directory is rejected by the
     * {@code predicate}, then there won't be any recursion into such directory.
     *
     * @param directoryPath the path to the directory to be listed
     * @param recursive     whether to include the contents of sub-directories
     * @param predicate     a {@link Predicate} of {@link FilePayload} used to filter the output list
     * @return a {@link List} of {@link FilePayload}. Might be empty but will never be null
     * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exists or is not a directory
     */
    List<FilePayload> list(String directoryPath, boolean recursive, Predicate<FilePayload> predicate);

    /**
     * Obtains the content and metadata of a file at a given path.
     * <p>
     * Locking can be actually enabled through the {@code lock} argument,
     * however, the extent of such lock will depend on the implementation.
     * What is guaranteed by passing {@code true} on the {@code lock} argument
     * is that {@code this} instance will not attempt to modify this file
     * until the {@link InputStream} returned by the {@link FilePayload#getContent()}
     * this method returns is closed or fully consumed. Some implementation might
     * actually perform a file system level locking which goes beyond the extend
     * of {@code this} instance or even mule. For some other file systems that
     * might be simply not possible and no extra assumptions are to be taken.
     * <p>
     * This method also makes a best effort to determine the mime type of the
     * file being read. If {@link ContentMetadata#isOutputModifiable()} return
     * {@code true} on the {@code contentMetadata} argument, a {@link MimetypesFileTypeMap}
     * will be used to make an educated guess on the file's mime type
     *
     * @param filePath        the path of the file you want to read
     * @param lock            whether or not to lock the file
     * @param contentMetadata a {@link ContentMetadata} to pass mimeType information of the file
     * @return the file's content and metadata on a {@link FilePayload} instance
     * @throws IllegalArgumentException if the file at the given path doesn't exists
     */
    FilePayload read(String filePath, boolean lock, ContentMetadata contentMetadata);

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
     * {@code null} or {@link NullPayload} contents are not allowed and will result
     * in an {@link IllegalArgumentException}.
     * <p>
     * If the directory on which the file is attempting to be written doesn't
     * exist, then the operation will either throw {@link IllegalArgumentException}
     * or create such folder depending on the value of the {@code createParentDirectory}.
     * <p>
     * If the file itself already exists, then the behavior depends on the supplied
     * {@code mode}.
     * <p>
     * This method also supports locking support depending on the value of the
     * {@code lock} argument, but following the same rules and considerations
     * as described in the {@link #read(String, boolean, ContentMetadata)} method
     *
     * @param filePath              the path of the file to be written
     * @param content               the content to be written into the file
     * @param mode                  a {@link FileWriteMode}
     * @param event                 the {@link MuleEvent} which processing triggers this operation
     * @param lock                  whether or not to lock the file
     * @param createParentDirectory whether or not to attempt creating the parent directory if it doesn't exists.
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    void write(String filePath,
               Object content,
               FileWriteMode mode,
               MuleEvent event,
               boolean lock,
               boolean createParentDirectory);

    /**
     * Copies the file at the {@code sourcePath} into the {@code targetPath}.
     * <p>
     * If {@code targetPath} doesn't exists, and neither does its parent,
     * then an attempt will be made to create depending on the value of the
     * {@code createParentFolder} argument. If such argument is {@false},
     * then an {@link IllegalArgumentException} will be thrown.
     * <p>
     * It is also possible to use the {@code targetPath} to specify that
     * the copied file should also be renamed. For example, if {@code sourcePath}
     * has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to
     * <i>a/c/test.json</i>, then the file will indeed be copied to the
     * <i>a/c/</i> directory but renamed as <i>test.json</i>
     * <p>
     * If the target file already exists, then it will be overwritten if the
     * {@code overwrite} argument is {@code true}. Otherwise, {@link IllegalArgumentException}
     * will be thrown
     * <p>
     * As for the {@code sourcePath}, it can either be a file or a directory.
     * If it points to a directory, then it will be copied recursively
     *
     * @param sourcePath         the path to the file to be copied
     * @param targetPath         the target directory
     * @param overwrite          whether or not overwrite the file if the target destination already exists.
     * @param createParentFolder whether or not to attempt creating the parent directory if it doesn't exists.
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    void copy(String sourcePath, String targetPath, boolean overwrite, boolean createParentFolder);

    /**
     * Moves the file at the {@code sourcePath} into the {@code targetPath}.
     * <p>
     * If {@code targetPath} doesn't exists, and neither does its parent,
     * then an attempt will be made to create depending on the value of the
     * {@code createParentFolder} argument. If such argument is {@false},
     * then an {@link IllegalArgumentException} will be thrown.
     * <p>
     * It is also possible to use the {@code targetPath} to specify that
     * the moved file should also be renamed. For example, if {@code sourcePath}
     * has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to
     * <i>a/c/test.json</i>, then the file will indeed be moved to the
     * <i>a/c/</i> directory but renamed as <i>test.json</i>
     * <p>
     * If the target file already exists, then it will be overwritten if the
     * {@code overwrite} argument is {@code true}. Otherwise, {@link IllegalArgumentException}
     * will be thrown
     * <p>
     * As for the {@code sourcePath}, it can either be a file or a directory.
     * If it points to a directory, then it will be moved recursively
     *
     * @param sourcePath         the path to the file to be copied
     * @param targetPath         the target directory
     * @param overwrite          whether or not overwrite the file if the target destination already exists.
     * @param createParentFolder whether or not to attempt creating the parent directory if it doesn't exists.
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    void move(String sourcePath, String targetPath, boolean overwrite, boolean createParentFolder);

    /**
     * Deletes the file pointed by {@code filePath}, provided that it's not locked
     *
     * @param filePath the path to the file to be deleted
     * @throws IllegalArgumentException if {@code filePath} doesn't exists or is locked
     */
    void delete(String filePath);

    /**
     * Renames the file pointed by {@code filePath} to the provided {@code newName}
     *
     * @param filePath the path to the file to be renamed
     * @param newName  the file's new name
     */
    void rename(String filePath, String newName);

    /**
     * Creates a new directory of the given {@code directoryName} as a child
     * of the provided {@code basePath}
     *
     * @param basePath      the directory which contains the directory to be created
     * @param directoryName the new directory's new name
     */
    void createDirectory(String basePath, String directoryName);
}
