/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Ensures that the file is moved to the archiveFile folder after a successful
 * consumption of the file
 * 
 * @author Magnus Larsson
 */
public class SftpFileArchiveInputStream extends FileInputStream implements ErrorOccurredDecorator
{
    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(SftpFileArchiveInputStream.class);

    private File file;
    private File archiveFile;
    private boolean errorOccured = false;

    // Log every 10 000 000 bytes read at debug-level
    // Good if really large files are transferred and you tend to get nervous by not
    // seeing any progress in the logfile...
    private static final int LOG_BYTE_INTERVAL = 10000000;
    private long bytesRead = 0;
    private long nextLevelToLogBytesRead = LOG_BYTE_INTERVAL;

    public SftpFileArchiveInputStream(File file) throws FileNotFoundException
    {
        super(file);

        this.file = file;
        this.archiveFile = null;
    }

    public SftpFileArchiveInputStream(File file, File archiveFile) throws FileNotFoundException
    {
        super(file);

        this.file = file;
        this.archiveFile = archiveFile;
    }

    @Override
    public int read() throws IOException
    {
        logReadBytes(1);
        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        logReadBytes(len);
        return super.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        logReadBytes(b.length);
        return super.read(b);
    }

    public void close() throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Closing the stream for the file " + file);
        }
        super.close();

        if (!errorOccured && archiveFile != null)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Move archiveTmpSendingFile (" + file + ") to archiveFolder (" + archiveFile
                            + ")...");
            }
            FileUtils.moveFile(file, archiveFile);
        }
    }

    public void setErrorOccurred()
    {
        if (logger.isDebugEnabled()) logger.debug("setErrorOccurred() called");
        this.errorOccured = true;
    }

    private void logReadBytes(int newBytesRead)
    {
        if (!logger.isDebugEnabled()) return;

        this.bytesRead += newBytesRead;
        if (this.bytesRead >= nextLevelToLogBytesRead)
        {
            logger.debug("Read " + this.bytesRead + " bytes and couting...");
            nextLevelToLogBytesRead += LOG_BYTE_INTERVAL;
        }
    }

}
