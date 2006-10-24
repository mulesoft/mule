/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.file;

import org.apache.commons.io.IOUtils;
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.DefaultXASession;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TransactedFileSession extends DefaultXASession implements FileSession
{

    public TransactedFileSession(AbstractXAResourceManager resourceManager)
    {
        super(resourceManager);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.file.FileSession#openInputStream(java.io.File)
     */
    public FileInputStream openInputStream(File f) throws IOException
    {
        if (localContext != null)
        {
            // TODO
            return null;
        }
        else
        {
            return new FileInputStream(f);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.file.FileSession#openOutputStream(java.io.File,
     *      boolean)
     */
    public FileOutputStream openOutputStream(File f, boolean append) throws IOException
    {
        if (localContext != null)
        {
            // TODO
            return null;
        }
        else
        {
            return new FileOutputStream(f, append);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.file.FileSession#openOutputStream(java.io.File)
     */
    public FileOutputStream openOutputStream(File f) throws IOException
    {
        return openOutputStream(f, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.file.FileSession#mkdir(java.io.File)
     */
    public boolean mkdir(File f) throws IOException
    {
        if (localContext != null)
        {
            // TODO
            return false;
        }
        else
        {
            return f.mkdir();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.file.FileSession#openRandomAccess(java.io.File,
     *      java.lang.String)
     */
    public RandomAccessFile openRandomAccess(File f, String mode) throws IOException
    {
        if (localContext != null)
        {
            // TODO
            return null;
        }
        else
        {
            return new RandomAccessFile(f, mode);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.file.FileSession#delete(java.io.File)
     */
    public void delete(File f) throws IOException
    {
        if (localContext != null)
        {
            // TODO
        }
        else
        {
            if (!f.delete())
            {
                throw new DeleteException(f);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.file.FileSession#copy(java.io.File, java.io.File)
     */
    public void copy(File source, File dest) throws IOException
    {
        if (dest.exists())
        {
            delete(dest);
        }
        InputStream is = null;
        OutputStream os = null;
        try
        {
            is = openInputStream(source);
            try
            {
                os = openOutputStream(dest);
                IOUtils.copy(is, os);
            }
            finally
            {
                IOUtils.closeQuietly(os);
            }
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.file.FileSession#rename(java.io.File,
     *      java.io.File)
     */
    public void rename(File source, File dest) throws IOException
    {
        copy(source, dest);
        delete(dest);
    }

}
