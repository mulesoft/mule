/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ThreadSafeAccess;
import org.mule.umo.MessagingException;

import java.io.File;

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

    public FileContentsMessageAdapter(Object message) throws MessagingException
    {
        super(message);
    }

    public FileContentsMessageAdapter(FileContentsMessageAdapter template)
    {
        super(template);
    }
    
    protected void setMessage(File message) throws MessagingException
    {
        super.setMessage(message);
        // force reading of file (lazy loading would be really, really complicated)
        this.getPayload();
    }

    public Object getPayload()
    {
        synchronized (this)
        {
            try
            {
                return this.getPayloadAsBytes();
            }
            catch (Exception noPayloadException)
            {
                throw new MuleRuntimeException(CoreMessages.failedToReadPayload(), noPayloadException);
            }
        }
    }
    
    public ThreadSafeAccess newThreadCopy()
    {
        return new FileContentsMessageAdapter(this);
    }
}
