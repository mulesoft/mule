/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.DefaultMuleException;
import org.mule.transport.file.i18n.FileMessages;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This implementation is used when streaming and will move or delete the source file
 * when the stream is closed.
 */
class ReceiverFileInputStream extends FileInputStream
{
    protected transient Log logger = LogFactory.getLog(getClass());

    private File currentFile;
    private boolean deleteOnClose;
    private File moveToOnClose;
    private boolean streamProcessingError;
    private InputStreamCloseListener closeListener;
    private AtomicBoolean alreadyClosed = new AtomicBoolean(false);
    private AtomicBoolean alreadyNotified = new AtomicBoolean(false);

    public ReceiverFileInputStream(File currentFile, boolean deleteOnClose, File moveToOnClose)
        throws FileNotFoundException
    {
        super(currentFile);
        this.currentFile = currentFile;
        this.deleteOnClose = deleteOnClose;
        this.moveToOnClose = moveToOnClose;
    }

    public ReceiverFileInputStream(File sourceFile, boolean deleteOnClose, File destinationFile, InputStreamCloseListener closeListener) throws FileNotFoundException
    {
        this(sourceFile, deleteOnClose, destinationFile);
        this.closeListener = closeListener;
    }

    @Override
    public void close() throws IOException
    {
        super.close();

        if (!isStreamProcessingError() && !alreadyClosed.getAndSet(true))
        {
            if (moveToOnClose != null)
            {
                if (!FileUtils.moveFileWithCopyFallback(currentFile, moveToOnClose))
                {
                    logger.warn(String.format("Failed to move file from %s to %s\n", currentFile.getPath(), moveToOnClose.getPath()));
                }
            }
            else if (deleteOnClose)
            {
                if (!currentFile.delete())
                {
                    throw new IOException(new DefaultMuleException(FileMessages.failedToDeleteFile(currentFile)));
                }
            }
        }

        if (closeListener != null && !alreadyNotified.getAndSet(true))
        {
            closeListener.fileClose(currentFile);
        }
    }

    public File getCurrentFile()
    {
        return currentFile;
    }

    public boolean isStreamProcessingError()
    {
        return streamProcessingError;
    }

    public void setStreamProcessingError(boolean streamProcessingError)
    {
        this.streamProcessingError = streamProcessingError;
    }
}
