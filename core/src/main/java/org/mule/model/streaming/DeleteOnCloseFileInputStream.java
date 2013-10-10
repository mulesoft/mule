/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.model.streaming;

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
                file.delete();
                file = null;
            }
        }
    }
}
