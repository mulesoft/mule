/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.mule.api.DefaultMuleException;
import org.mule.transport.file.i18n.FileMessages;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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

        if (!streamProcessingError)
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
                    try
                    {
                        throw new DefaultMuleException(FileMessages.failedToDeleteFile(currentFile));
                    }
                    catch (DefaultMuleException e)
                    {
                        IOException e2 = new IOException();
                        e2.initCause(e);
                        throw e2;
                    }
                }
            }
        }
        if (closeListener != null)
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
