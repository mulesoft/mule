/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.file.api;

import org.mule.api.MuleEvent;
import org.mule.api.temporary.MuleMessage;
import org.mule.api.transport.OutputHandler;
import org.mule.extension.annotation.api.DataTypeParameters;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.module.extension.file.api.matcher.NullFilePayloadPredicate;
import org.mule.transport.NullPayload;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import javax.activation.MimetypesFileTypeMap;

/**
 * Basic set of operations for extensions which perform operations
 * over a generic file system
 *
 * @since 4.0
 */
//TODO: MULE-9215
public class StandardFileSystemOperations
{

    /**
     * Lists all the files in the {@code directoryPath} which match the given {@code matcher}.
     * <p>
     * If the listing encounters a directory, the output list will include its contents depending
     * on the value of the {@code recursive} parameter.
     * <p>
     * If {@code recursive} is set to {@code true} but a found directory is rejected by the
     * {@code matcher}, then there won't be any recursion into such directory.
     *
     * @param directoryPath the path to the directory to be listed
     * @param recursive     whether to include the contents of sub-directories. Defaults to {@code false}
     * @param matcher       a matcher used to filter the output list
     * @return a {@link List} of {@link FilePayload}. Might be empty but will never be null
     * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exists or is not a directory
     */
    @Operation
    //TODO: MULE-9233
    public List<FilePayload> list(@Connection FileSystem fileSystem,
                                  @Optional String directoryPath,
                                  @Optional(defaultValue = "false") boolean recursive,
                                  @Optional FilePayloadPredicateBuilder matcher)
    {
        return fileSystem.list(directoryPath, recursive, getPredicate(matcher));
    }

    /**
     * Obtains the content and metadata of a file at a given path.
     * <p>
     * If the {@code lock} parameter is set to {@code true}, then a file system
     * level lock will be placed on the file until the {@link InputStream} returned
     * by the {@link FilePayload#getContent()} this operation returns is closed or
     * fully consumed. Because the lock is actually provided by the host file system,
     * its behavior might change depending on the mounted drive and the operation system
     * on which mule is running. Take that into consideration before blindly relying on this
     * lock.
     * <p>
     * This method also makes a best effort to determine the mime type of the
     * file being read. A {@link MimetypesFileTypeMap} will be used to make an
     * educated guess on the file's mime type. The user also has
     * the chance to force the output enconding and mimeType through the {@code outputEncoding}
     * and {@code outputMimeType} optional parameters.
     *
     *
     * @param fileSystem      a reference to the host {@link FileSystem}
     * @param message         the incoming {@link MuleMessage}
     * @param path            the path to the file to be read
     * @param lock            whether or not to lock the file. Defaults to {@code false}
     * @return a MuleMessage with the file's content and metadata on a {@link FilePayload} instance
     * @throws IllegalArgumentException if the file at the given path doesn't exists
     */
    @Operation
    @DataTypeParameters
    public MuleMessage read(@Connection FileSystem fileSystem,
                            MuleMessage message,
                            String path,
                            @Optional(defaultValue = "false") boolean lock)
    {
        return fileSystem.read(message, path, lock);
    }

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
     * This operation also supports locking support depending on the value of the
     * {@code lock} argument, but following the same rules and considerations
     * as described in the read operation.
     *
     * @param fileSystem            a reference to the host {@link FileSystem}
     * @param path                  the path of the file to be written
     * @param content               the content to be written into the file. Defaults to the current {@link MuleMessage} payload
     * @param mode                  a {@link FileWriteMode}. Defaults to {@code OVERWRITE}
     * @param lock                  whether or not to lock the file. Defaults to {@code false}
     * @param createParentDirectory whether or not to attempt creating the parent directory if it doesn't exists.
     * @param event                 The current {@link MuleEvent}
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    @Operation
    public void write(@Connection FileSystem fileSystem,
                      String path,
                      @Optional(defaultValue = "#[payload]") Object content,
                      @Optional(defaultValue = "OVERWRITE") FileWriteMode mode,
                      @Optional(defaultValue = "false") boolean lock,
                      @Optional(defaultValue = "true") boolean createParentDirectory,
                      MuleEvent event)
    {
        if (content == null || content instanceof NullPayload)
        {
            throw new IllegalArgumentException("Cannot write a null content");
        }

        fileSystem.write(path, content, mode, event, lock, createParentDirectory);
    }

    /**
     * Copies the file at the {@code sourcePath} into the {@code targetPath}.
     * <p>
     * If {@code targetPath} doesn't exists, and neither does its parent,
     * then an attempt will be made to create depending on the value of the
     * {@code createParentFolder} argument. If such argument is {@false},
     * then an {@link IllegalArgumentException} will be thrown.
     * <p>
     * If the target file already exists, then it will be overwritten if the
     * {@code overwrite} argument is {@code true}. Otherwise, {@link IllegalArgumentException}
     * will be thrown.
     * <p>
     * It is also possible to use the {@code targetPath} to specify that
     * the copied file should also be renamed. For example, if {@code sourcePath}
     * has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to
     * <i>a/c/test.json</i>, then the file will indeed be copied to the
     * <i>a/c/</i> directory but renamed as <i>test.json</i>
     * <p>
     * As for the {@code sourcePath}, it can either be a file or a directory.
     * If it points to a directory, then it will be copied recursively.
     *
     * @param fileSystem         a reference to the host {@link FileSystem}
     * @param sourcePath         the path to the file to be copied
     * @param targetPath         the target directory
     * @param overwrite          whether or not overwrite the file if the target destination already exists.
     * @param createParentFolder whether or not to attempt creating the parent directory if it doesn't exists.
     * @param event              the {@link MuleEvent} which triggered this operation
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    @Operation
    public void copy(@Connection FileSystem fileSystem,
                     String sourcePath,
                     String targetPath,
                     @Optional(defaultValue = "false") boolean overwrite,
                     @Optional(defaultValue = "true") boolean createParentFolder,
                     MuleEvent event)
    {
        fileSystem.copy(sourcePath, targetPath, overwrite, createParentFolder, event);
    }

    /**
     * Moves the file at the {@code sourcePath} into the {@code targetPath}.
     * <p>
     * If {@code targetPath} doesn't exists, and neither does its parent,
     * then an attempt will be made to create depending on the value of the
     * {@code createParentFolder} argument. If such argument is {@false},
     * then an {@link IllegalArgumentException} will be thrown.
     * <p>
     * If the target file already exists, then it will be overwritten if the
     * {@code overwrite} argument is {@code true}. Otherwise, {@link IllegalArgumentException}
     * will be thrown.
     * <p>
     * It is also possible to use the {@code targetPath} to specify that
     * the moved file should also be renamed. For example, if {@code sourcePath}
     * has the value <i>a/b/test.txt</i> and {@code targetPath} is assigned to
     * <i>a/c/test.json</i>, then the file will indeed be copied to the
     * <i>a/c/</i> directory but renamed as <i>test.json</i>
     * <p>
     * As for the {@code sourcePath}, it can either be a file or a directory.
     * If it points to a directory, then it will be moved recursively.
     *
     * @param fileSystem         a reference to the host {@link FileSystem}
     * @param sourcePath         the path to the file to be copied
     * @param targetDirectory    the target directory
     * @param overwrite          whether or not overwrite the file if the target destination already exists.
     * @param createParentFolder whether or not to attempt creating the parent directory if it doesn't exists.
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    @Operation
    public void move(@Connection FileSystem fileSystem,
                     String sourcePath,
                     String targetDirectory,
                     @Optional(defaultValue = "false") boolean overwrite,
                     @Optional(defaultValue = "true") boolean createParentFolder)
    {
        fileSystem.move(sourcePath, targetDirectory, overwrite, createParentFolder);
    }

    /**
     * Deletes the file pointed by {@code path}, provided that it's not locked
     *
     * @param fileSystem a reference to the host {@link FileSystem}
     * @param path       the path to the file to be deleted
     * @throws IllegalArgumentException if {@code filePath} doesn't exists or is locked
     */
    @Operation
    public void delete(@Connection FileSystem fileSystem, String path)
    {
        fileSystem.delete(path);
    }

    /**
     * Renames the file pointed by {@code path} to the name provided on
     * the {@code to} parameter
     *
     * @param fileSystem a reference to the host {@link FileSystem}
     * @param path       the path to the file to be renamed
     * @param to         the file's new name
     */
    @Operation
    public void rename(@Connection FileSystem fileSystem, String path, String to)
    {
        fileSystem.rename(path, to);
    }

    /**
     * Creates a new directory of the given {@code directoryName} as a child
     * of the provided {@code basePath}
     *
     * @param fileSystem    a reference to the host {@link FileSystem}
     * @param basePath      the directory which contains the directory to be created
     * @param directoryName the new directory's new name
     */
    @Operation
    public void createDirectory(@Connection FileSystem fileSystem, @Optional String basePath, String directoryName)
    {
        fileSystem.createDirectory(basePath, directoryName);
    }

    private Predicate<FilePayload> getPredicate(FilePayloadPredicateBuilder builder)
    {
        return builder != null ? builder.build() : new NullFilePayloadPredicate();
    }

}
