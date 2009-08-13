/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This implementation is used when streaming and will move or delete the source file
 * when the stream is closed.
 */
class ReceiverFileInputStream extends FileInputStream
{
    private static Log log = LogFactory.getLog(ReceiverFileInputStream.class);
    
    private File currentFile;
    private boolean deleteOnClose;
    private File moveToOnClose;

    public ReceiverFileInputStream(File currentFile, boolean deleteOnClose, File moveToOnClose)
        throws FileNotFoundException
    {
        super(currentFile);
        this.currentFile = currentFile;
        this.deleteOnClose = deleteOnClose;
        this.moveToOnClose = moveToOnClose;
    }

    @Override
    public void close() throws IOException
    {
        super.close();

        if (moveToOnClose != null)
        {
            boolean success = FileUtils.moveFile(currentFile, moveToOnClose);
            if (success == false)
            {
                log.error("Could not move " + currentFile.getAbsolutePath()
                    + " to " + moveToOnClose.getAbsolutePath());
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

    public File getCurrentFile()
    {
        return currentFile;
    }

}
