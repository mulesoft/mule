/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.module.extension.file.api.AbstractFileAttributes;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.apache.commons.net.ftp.FTPFile;

/**
 * Implementation of {@link AbstractFileAttributes} for files
 * read from a FTP server.
 *
 * @since 4.0
 */
//TODO: MULE-9232
public final class FtpFileAttributes extends AbstractFileAttributes
{

    private final FTPFile ftpFile;

    /**
     * Creates a new instance
     *
     * @param path         the file's {@link Path}
     * @param ftpFile      the {@link FTPFile} which represents the file on the FTP server
     */
    public FtpFileAttributes(Path path, FTPFile ftpFile)
    {
        super(path);
        this.ftpFile = ftpFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime getLastModifiedTime()
    {
        return asDateTime(ftpFile.getTimestamp().toInstant());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime getLastAccessTime()
    {
        return getLastModifiedTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime getCreationTime()
    {
        return getLastModifiedTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return ftpFile.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize()
    {
        return ftpFile.getSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegularFile()
    {
        return ftpFile.isFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory()
    {
        return ftpFile.isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSymbolicLink()
    {
        return ftpFile.isSymbolicLink();
    }
}
