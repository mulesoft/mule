/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model.streaming;

import static org.mule.util.FileUtils.deleteFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * FileInputStream which deletes the underlying file when the stream is closed.
 */
public class DeleteOnCloseFileInputStream extends FileInputStream
{
    private File file;

    /**
     * Builds a {@link DeleteOnCloseFileInputStream}.
     *
     * @param file #see
     * @throws java.io.FileNotFoundException If there is a problem regarding the file.
     */
    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException
    {
        super(file);
        this.file = file;
    }

    public void close() throws IOException
    {
        try
        {
            super.close();
        }
        finally
        {
            if (file != null)
            {
                deleteFile(file);
                file = null;
            }
        }
    }
}
