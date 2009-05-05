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

import org.mule.api.MessagingException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.ThreadSafeAccess;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.IOUtils;

import java.io.FileInputStream;

/**
 * <code>FileContentsMessageAdapter</code> provides a wrapper for file data. Users
 * can obtain the contents of the message through the payload property and can get
 * the filename and directory in the properties using PROPERTY_FILENAME and
 * PROPERTY_DIRECTORY.
 */
public class FileContentsMessageAdapter extends FileMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7368719494535568721L;

    private byte[] contents = null;

    public FileContentsMessageAdapter(Object message) throws MessagingException
    {
        super(message);
        this.getPayload();
    }

    public FileContentsMessageAdapter(FileContentsMessageAdapter template)
    {
        super(template);
        contents = template.contents;
        this.getPayload();
    }

    public Object getPayload()
    {
        if (contents == null)
        {
            synchronized (this)
            {
                try
                {
                    if (fileInputStream == null)
                    {
                        fileInputStream = new FileInputStream(file);
                    }
                    contents = IOUtils.toByteArray(fileInputStream);
                    fileInputStream.close();
                    
                    // discard the fileInputStream here so that the MuleEvent referencing this
                    // message adapter can be serialized properly
                    fileInputStream = null;
                }
                catch (Exception noPayloadException)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToReadPayload(), noPayloadException);
                }
            }
        }
        return contents;
    }

    public ThreadSafeAccess newThreadCopy()
    {
        return new FileContentsMessageAdapter(this);
    }
}
