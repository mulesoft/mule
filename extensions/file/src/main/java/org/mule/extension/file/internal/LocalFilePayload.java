/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.file.internal.lock.NullPathLock;
import org.mule.module.extension.file.FilePayload;
import org.mule.module.extension.file.PathLock;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Implementation of {@link FilePayload} for files obtained
 * from a local file system.
 *
 * @since 4.0
 */
public final class LocalFilePayload implements FilePayload, Closeable
{

    private final Path path;
    private final PathLock lock;

    private BasicFileAttributes attributes = null;
    private InputStream content;

    /**
     * Creates a new instance
     *
     * @param path a {@link Path} pointing to the represented file
     */
    LocalFilePayload(Path path)
    {
        this(path, new NullPathLock());
    }

    /**
     * Creates a new instance.
     * <p>
     * This constructor allows providing a {@link PathLock}
     * to be released when the {@link #close()} method is invoked
     * or when the {@link InputStream} returned by the
     * {@link #getContent()} method is closed or fully consumed.
     * <p>
     * It is the responsability of whomever invokes this constructor
     * to invoke (if necessary) the {@link PathLock#tryLock()} method
     * on the supplied {@code lock}.
     *
     * @param path a {@link Path} pointing to the represented file
     * @param a    {@link PathLock}
     */
    public LocalFilePayload(Path path, PathLock lock)
    {
        this.path = path;
        this.lock = lock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime getLastModifiedTime()
    {
        return asDateTime(getAttributes().lastModifiedTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime getLastAccessTime()
    {
        return asDateTime(getAttributes().lastAccessTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime getCreationTime()
    {
        return asDateTime(getAttributes().creationTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize()
    {
        return getAttributes().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegularFile()
    {
        return getAttributes().isRegularFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory()
    {
        return getAttributes().isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSymbolicLink()
    {
        return getAttributes().isSymbolicLink();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath()
    {
        return path.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilename()
    {
        return path.getFileName().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized InputStream getContent()
    {
        if (content == null)
        {
            try
            {
                content = new FileInputStream(Files.newBufferedReader(path), lock);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(createStaticMessage("Could not open file " + path), e);
            }
        }

        return content;
    }

    /**
     * Closes the {@link InputStream} returned by {@link #getContent()}
     * and releases the {@link #lock} if supplied
     *
     * @throws IOException
     */
    @Override
    public synchronized void close() throws IOException
    {
        try
        {
            closeStream();
        }
        finally
        {
            lock.release();
        }
    }

    private void closeStream() throws IOException
    {
        if (content != null)
        {
            content.close();
        }
    }

    public boolean isLocked()
    {
        return lock.isLocked();
    }

    private synchronized BasicFileAttributes getAttributes()
    {
        if (attributes == null)
        {
            try
            {
                attributes = Files.readAttributes(path, BasicFileAttributes.class);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(createStaticMessage("Could not read attributes for file " + path), e);
            }
        }

        return attributes;
    }

    private LocalDateTime asDateTime(FileTime fileTime)
    {
        return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
    }
}
