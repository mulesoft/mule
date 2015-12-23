/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.module.extension.file.api.PathLock;
import org.mule.module.extension.file.api.AbstractFilePayload;
import org.mule.module.extension.file.api.NullPathLock;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.apache.commons.net.ftp.FTPFile;

/**
 * Implementation of {@link AbstractFilePayload} for files
 * read from a FTP server.
 *
 * @since 4.0
 */
//TODO: MULE-9232
public final class FtpFilePayload extends AbstractFilePayload
{

    private final FTPFile ftpFile;
    private final FtpConnector ftpConnector;

    /**
     * Creates a new instance
     *
     * @param path         the file's {@link Path}
     * @param ftpFile      the {@link FTPFile} which represents the file on the FTP server
     * @param ftpConnector the configuring {@link FtpConnector}
     */
    public FtpFilePayload(Path path, FTPFile ftpFile, FtpConnector ftpConnector)
    {
        this(path, ftpFile, ftpConnector, new NullPathLock());
    }

    /**
     * Creates a new instance which is a shallow copy of the
     * {@code original}
     *
     * @param original the {@link FtpFilePayload} to copy
     * @param lock     the {@link PathLock} to use
     */
    public FtpFilePayload(FtpFilePayload original, PathLock lock)
    {
        this(original.path, original.ftpFile, original.ftpConnector, lock);
    }

    private FtpFilePayload(Path path, FTPFile ftpFile, FtpConnector ftpConnector, PathLock lock)
    {
        super(path, lock);
        this.ftpFile = ftpFile;
        this.ftpConnector = ftpConnector;
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

    /**
     * {@inheritDoc}
     *
     * @return a {@link FtpInputStream}
     */
    @Override
    protected InputStream doGetContent() throws Exception
    {
        return FtpInputStream.newInstance(ftpConnector, this, lock);
    }
}
