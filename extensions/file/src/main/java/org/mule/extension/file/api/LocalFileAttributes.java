/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.api;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.module.extension.file.api.AbstractFileAttributes;
import org.mule.module.extension.file.api.FileAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Implementation of {@link FileAttributes} for files obtained
 * from a local file system.
 *
 * @since 4.0
 */
public final class LocalFileAttributes extends AbstractFileAttributes
{

    private BasicFileAttributes attributes = null;

    /**
     * {@inheritDoc}
     */
    public LocalFileAttributes(Path path)
    {
        super(path);
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
