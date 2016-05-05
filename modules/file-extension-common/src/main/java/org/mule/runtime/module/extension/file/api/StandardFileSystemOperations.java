/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.annotation.DataTypeParameters;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.extension.file.api.matcher.NullFilePayloadPredicate;

import java.io.InputStream;
import java.util.Iterator;
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
     * @param message       the {@link MuleMessage} on which this operation was triggered
     * @param matchWith     a matcher used to filter the output list
     * @return a {@link TreeNode} object representing the listed directory
     * @throws IllegalArgumentException if {@code directoryPath} points to a file which doesn't exists or is not a directory
     */
    public TreeNode list(@Connection FileSystem fileSystem,
                         @Optional String directoryPath,
                         @Optional(defaultValue = "false") boolean recursive,
                         MuleMessage<?, ?> message,
                         @Optional FilePredicateBuilder matchWith)
    {
        return fileSystem.list(directoryPath, recursive, message, getPredicate(matchWith));
    }

    /**
     * Obtains the content and metadata of a file at a given path. The operation itself
     * returns a {@link MuleMessage} which payload is a {@link InputStream} with the
     * file's content, and the metadata is represent as a {@link FileAttributes} object
     * that's placed as the message {@link MuleMessage#getAttributes() attributes}.
     * <p>
     * If the {@code lock} parameter is set to {@code true}, then a file system
     * level lock will be placed on the file until the input stream this operation
     * returns is closed or fully consumed. Because the lock is actually provided by
     * the host file system, its behavior might change depending on the mounted drive
     * and the operation system on which mule is running. Take that into consideration
     * before blindly relying on this lock.
     * <p>
     * This method also makes a best effort to determine the mime type of the
     * file being read. A {@link MimetypesFileTypeMap} will be used to make an
     * educated guess on the file's mime type. The user also has
     * the chance to force the output enconding and mimeType through the {@code outputEncoding}
     * and {@code outputMimeType} optional parameters.
     *
     * @param fileSystem a reference to the host {@link FileSystem}
     * @param message    the incoming {@link MuleMessage}
     * @param path       the path to the file to be read
     * @param lock       whether or not to lock the file. Defaults to {@code false}
     * @return the file's content and metadata on a {@link FileAttributes} instance
     * @throws IllegalArgumentException if the file at the given path doesn't exists
     */
    @DataTypeParameters
    public MuleMessage<InputStream, FileAttributes> read(@Connection FileSystem fileSystem,
                                                         MuleMessage<?, ?> message,
                                                         String path,
                                                         @Optional(defaultValue = "false") boolean lock)
    {
        return fileSystem.read(message, path, lock);
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
     * {@code null} or {@link NullPayload} contents are not allowed and will result
     * in an {@link IllegalArgumentException}.
     * <p>
     * To support pass-through scenarios, the {@code path} attribute is optional. If not provided,
     * then the current {@link MuleMessage#getAttributes()} value will be tested to be an
     * instance of {@link FileAttributes}, in which case {@link FileAttributes#getPath()}
     * will be used. If that's not the case, then an {@link IllegalArgumentException} will
     * be thrown.
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
     * @param fileSystem              a reference to the host {@link FileSystem}
     * @param path                    the path of the file to be written
     * @param content                 the content to be written into the file. Defaults to the current {@link MuleMessage} payload
     * @param mode                    a {@link FileWriteMode}. Defaults to {@code OVERWRITE}
     * @param lock                    whether or not to lock the file. Defaults to {@code false}
     * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
     * @param event                   The current {@link MuleEvent}
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    public void write(@Connection FileSystem fileSystem,
                      @Optional String path,
                      @Optional(defaultValue = "#[payload]") Object content,
                      @Optional(defaultValue = "OVERWRITE") FileWriteMode mode,
                      @Optional(defaultValue = "false") boolean lock,
                      @Optional(defaultValue = "true") boolean createParentDirectories,
                      MuleEvent event)
    {
        if (content == null || content instanceof NullPayload)
        {
            throw new IllegalArgumentException("Cannot write a null content");
        }

        path = resolvePath(path, event, "path");
        fileSystem.write(path, content, mode, event, lock, createParentDirectories);
    }

    /**
     * Copies the file at the {@code sourcePath} into the {@code targetPath}.
     * <p>
     * To support pass-through scenarios, the {@code sourcePath} attribute is optional. If not provided,
     * then the current {@link MuleMessage#getAttributes()} value will be tested to be an
     * instance of {@link FileAttributes}, in which case {@link FileAttributes#getPath()}
     * will be used. If that's not the case, then an {@link IllegalArgumentException} will
     * be thrown.
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
     * @param fileSystem              a reference to the host {@link FileSystem}
     * @param sourcePath              the path to the file to be copied
     * @param targetPath              the target directory
     * @param overwrite               whether or not overwrite the file if the target destination already exists.
     * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
     * @param event                   the {@link MuleEvent} which triggered this operation
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    public void copy(@Connection FileSystem fileSystem,
                     @Optional String sourcePath,
                     String targetPath,
                     @Optional(defaultValue = "false") boolean overwrite,
                     @Optional(defaultValue = "true") boolean createParentDirectories,
                     MuleEvent event)
    {
        sourcePath = resolvePath(sourcePath, event, "sourcePath");
        fileSystem.copy(sourcePath, targetPath, overwrite, createParentDirectories, event);
    }

    /**
     * Moves the file at the {@code sourcePath} into the {@code targetPath}.
     * <p>
     * To support pass-through scenarios, the {@code sourcePath} attribute is optional. If not provided,
     * then the current {@link MuleMessage#getAttributes()} value will be tested to be an
     * instance of {@link FileAttributes}, in which case {@link FileAttributes#getPath()}
     * will be used. If that's not the case, then an {@link IllegalArgumentException} will
     * be thrown.
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
     * @param fileSystem              a reference to the host {@link FileSystem}
     * @param sourcePath              the path to the file to be copied
     * @param targetPath              the target directory
     * @param overwrite               whether or not overwrite the file if the target destination already exists.
     * @param createParentDirectories whether or not to attempt creating any parent directories which don't exists.
     * @param event                   The current {@link MuleEvent}
     * @throws IllegalArgumentException if an illegal combination of arguments is supplied
     */
    public void move(@Connection FileSystem fileSystem,
                     @Optional String sourcePath,
                     String targetPath,
                     @Optional(defaultValue = "false") boolean overwrite,
                     @Optional(defaultValue = "true") boolean createParentDirectories,
                     MuleEvent event)
    {
        sourcePath = resolvePath(sourcePath, event, "sourcePath");
        fileSystem.move(sourcePath, targetPath, overwrite, createParentDirectories);
    }

    /**
     * Deletes the file pointed by {@code path}, provided that it's not locked
     * <p>
     * To support pass-through scenarios, the {@code path} attribute is optional. If not provided,
     * then the current {@link MuleMessage#getAttributes()} value will be tested to be an
     * instance of {@link FileAttributes}, in which case {@link FileAttributes#getPath()}
     * will be used. If that's not the case, then an {@link IllegalArgumentException} will
     * be thrown.
     *
     * @param fileSystem a reference to the host {@link FileSystem}
     * @param path       the path to the file to be deleted
     * @param event      The current {@link MuleEvent}
     * @throws IllegalArgumentException if {@code filePath} doesn't exists or is locked
     */
    public void delete(@Connection FileSystem fileSystem, @Optional String path, MuleEvent event)
    {
        path = resolvePath(path, event, "path");
        fileSystem.delete(path);
    }

    /**
     * Renames the file pointed by {@code path} to the name provided on
     * the {@code to} parameter
     * <p>
     * To support pass-through scenarios, the {@code path} attribute is optional. If not provided,
     * then the current {@link MuleMessage#getAttributes()} value will be tested to be an
     * instance of {@link FileAttributes}, in which case {@link FileAttributes#getPath()}
     * will be used. If that's not the case, then an {@link IllegalArgumentException} will
     * be thrown.
     *
     * @param fileSystem a reference to the host {@link FileSystem}
     * @param path       the path to the file to be renamed
     * @param to         the file's new name
     * @param event      The current {@link MuleEvent}
     */
    public void rename(@Connection FileSystem fileSystem, @Optional String path, String to, MuleEvent event)
    {
        path = resolvePath(path, event, "path");
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
    public void createDirectory(@Connection FileSystem fileSystem, @Optional String basePath, String directoryName)
    {
        fileSystem.createDirectory(basePath, directoryName);
    }

    private String resolvePath(String path, MuleEvent event, String attributeName)
    {
        if (!StringUtils.isBlank(path))
        {
            return path;
        }

        MuleMessage<?, ?> message = event.getMessage().asNewMessage();
        if (message.getAttributes() instanceof FileAttributes)
        {
            return ((FileAttributes) message.getAttributes()).getPath();
        }

        throw new IllegalArgumentException(String.format("A %s was not specified and a default one could not be obtained from the current message attributes", attributeName));
    }

    private Predicate<FileAttributes> getPredicate(FilePredicateBuilder builder)
    {
        return builder != null ? builder.build() : new NullFilePayloadPredicate();
    }
}
