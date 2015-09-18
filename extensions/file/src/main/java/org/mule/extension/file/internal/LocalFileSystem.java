/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import static java.lang.String.format;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.api.runtime.ContentMetadata;
import org.mule.extension.api.runtime.ContentType;
import org.mule.extension.file.internal.lock.LocalPathLock;
import org.mule.extension.file.internal.lock.NullPathLock;
import org.mule.module.extension.file.FilePayload;
import org.mule.module.extension.file.FileSystem;
import org.mule.module.extension.file.FileWriteMode;
import org.mule.module.extension.file.PathLock;
import org.mule.module.extension.file.internal.FileContentWrapper;
import org.mule.transport.NullPayload;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link FileSystem} for file systems
 * mounted on the host operating system.
 * <p>
 * Whenever the {@link FileSystem} contract refers to locking,
 * this implementation will resolve through a {@link LocalPathLock},
 * which produces file system level locks which rely on the host
 * operating system.
 * <p>
 * Also, for any method returning {@link FilePayload} instances,
 * instances of {@link LocalFilePayload} will be returned
 *
 * @since 4.0
 */
public final class LocalFileSystem implements FileSystem
{

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFileSystem.class);

    static RuntimeException exception(String message, Exception cause)
    {
        return new MuleRuntimeException(createStaticMessage(message), cause);
    }

    static RuntimeException exception(String message)
    {
        return new MuleRuntimeException(createStaticMessage(message));
    }

    private final FileConnector config;
    private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    /**
     * Creates a new instance
     *
     * @param config a {@link FileConnector} which acts as a config
     */
    public LocalFileSystem(FileConnector config)
    {
        this.config = config;
    }

    @Override
    public List<FilePayload> list(String directoryPath, boolean recursive, Predicate<FilePayload> matcher)
    {
        Path path = getExistingPath(directoryPath);
        if (!Files.isDirectory(path))
        {
            throw new IllegalArgumentException(format("Cannot list the path '%s' since it's not a directory", path));
        }

        List<FilePayload> accumulator = new LinkedList<>();
        doList(path.toFile(), accumulator, recursive, matcher);

        return accumulator;
    }

    private void doList(File parent, List<FilePayload> accumulator, boolean recursive, Predicate<FilePayload> matcher)
    {
        for (File child : parent.listFiles())
        {
            FilePayload payload = new LocalFilePayload(child.toPath());
            if (!matcher.test(payload))
            {
                continue;
            }

            accumulator.add(payload);
            if (child.isDirectory() && recursive)
            {
                doList(child, accumulator, recursive, matcher);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FilePayload read(String filePath, boolean lock, ContentMetadata contentMetadata)
    {
        Path path = getExistingPath(filePath);
        if (Files.isDirectory(path))
        {
            throw new IllegalArgumentException(format("Cannot read path '%s' since it's a directory", path));
        }

        FilePayload filePayload;
        if (lock)
        {
            filePayload = new LocalFilePayload(path, lock(path));
        }
        else
        {
            verifyNotLocked(path);
            filePayload = new LocalFilePayload(path);
        }

        updateContentMetadata(filePayload, contentMetadata);
        return filePayload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(String filePath, Object content, FileWriteMode mode, MuleEvent event, boolean lock, boolean createParentDirectory)
    {
        if (content == null || content instanceof NullPayload)
        {
            throw new IllegalArgumentException("Cannot write a null content");
        }

        Path path = getPath(filePath);

        assureParentFolderExists(path, createParentDirectory);

        final OpenOption[] openOptions = mode.getOpenOptions();
        PathLock pathLock = lock ? lock(path, openOptions) : new NullPathLock();

        try (OutputStream out = Files.newOutputStream(path, openOptions))
        {
            new FileContentWrapper(content).accept(new LocalFileWriterContentVisitor(out, event));
        }
        catch (Exception e)
        {
            throw exception(format("Exception was found writing to file '%s'", path), e);
        }
        finally
        {
            pathLock.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(String sourcePath, String targetDirectory, boolean overwrite, boolean createParentFolder)
    {
        Path source = getExistingPath(sourcePath);
        Path targetPath = getPath(targetDirectory);

        new CopyCommand(source, targetPath, overwrite, createParentFolder).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(String sourcePath, String targetDirectory, boolean overwrite, boolean createParentFolder)
    {
        Path source = getExistingPath(sourcePath);
        Path targetPath = getPath(targetDirectory);

        new MoveCommand(source, targetPath, overwrite, createParentFolder).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String filePath)
    {
        Path path = getExistingPath(filePath);

        LOGGER.debug("Preparing to delete '{}'", path);

        try
        {
            if (Files.isDirectory(path))
            {
                FileUtils.deleteTree(path.toFile());
            }
            else
            {
                verifyNotLocked(path);
                Files.deleteIfExists(path);
            }

            LOGGER.debug("Successfully deleted '{}'", path);
        }
        catch (IOException e)
        {
            throw exception(format("Could not delete '%s'", path), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rename(String filePath, String newName)
    {
        Path source = getExistingPath(filePath);
        Path target = source.getParent().resolve(newName);

        if (Files.exists(target))
        {
            throw new IllegalArgumentException(format("'%s' cannot be renamed because '%s' already exists", source, target));
        }

        try
        {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (Exception e)
        {
            throw exception(format("Exception was found renaming '%s' to '%s'", source, newName), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(String basePath, String directoryName)
    {
        if (StringUtils.isBlank(basePath))
        {
            basePath = config.getBaseDir();
        }

        Path base = getExistingPath(basePath);
        Path target = base.resolve(directoryName).toAbsolutePath();

        if (Files.exists(target))
        {
            throw new IllegalArgumentException(format("Directory '%s' already exists", target));
        }

        createDirectory(target.toFile());
    }

    private void createDirectory(File target)
    {
        try
        {
            if (!target.mkdirs())
            {
                throw new MuleRuntimeException(createStaticMessage(format("Directory '%s' could not be created", target)));
            }
        }
        catch (Exception e)
        {
            throw exception(format("Exception was found creating directory '%s'", target), e);
        }
    }

    private Path getExistingPath(String filePath)
    {
        Path path = getPath(filePath);
        if (Files.notExists(path))
        {
            throw pathNotFoundException(path);
        }

        return path;
    }

    private Path getPath(String filePath)
    {
        return Paths.get(config.getBaseDir()).resolve(filePath).toAbsolutePath();
    }

    private PathLock lock(Path path)
    {
        return lock(path, WRITE);
    }

    private PathLock lock(Path path, OpenOption... openOptions)
    {
        PathLock lock = new LocalPathLock(path, openOptions);
        if (!lock.tryLock())
        {
            throw exception(format("Could not lock file '%s' because it's already owned by another process", path));
        }

        return lock;
    }

    /**
     * Try to acquire a lock on a file and release it immediately. Usually used as a
     * quick check to see if another process is still holding onto the file, e.g. a
     * large file (more than 100MB) is still being written to.
     */
    private boolean isLocked(Path path)
    {
        PathLock lock = new LocalPathLock(path, StandardOpenOption.WRITE);
        try
        {
            return !lock.tryLock();
        }
        finally
        {
            lock.release();
        }
    }

    private void verifyNotLocked(Path path)
    {
        if (isLocked(path))
        {
            throw new IllegalStateException(format("File '%s' is locked by another process", path));
        }
    }

    private void assureParentFolderExists(Path path, boolean createParentFolder)
    {
        if (Files.exists(path))
        {
            return;
        }

        File parentFolder = path.getParent().toFile();
        if (!parentFolder.exists())
        {
            if (createParentFolder)
            {
                createDirectory(parentFolder);
            }
            else
            {
                throw new IllegalArgumentException(format("Cannot write to file '%s' because path to it doesn't exist. Consider setting the 'createParentFolder' attribute to 'true'", path));
            }
        }
    }

    private RuntimeException pathNotFoundException(Path path)
    {
        return new IllegalArgumentException(format("Path '%s' doesn't exists", path));
    }

    private void updateContentMetadata(FilePayload filePayload, ContentMetadata contentMetadata)
    {
        if (!contentMetadata.isOutputModifiable())
        {
            return;
        }

        String presumedMimeType = mimetypesFileTypeMap.getContentType(filePayload.getPath());
        ContentType outputContentType = contentMetadata.getOutputContentType();
        if (presumedMimeType != null)
        {
            contentMetadata.setOutputContentType(new ContentType(outputContentType.getEncoding(), presumedMimeType));
        }
    }
}
