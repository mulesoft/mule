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
import org.mule.api.ThreadSafeAccess;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.transport.AbstractMessageAdapter;

import java.io.File;
import java.io.InputStream;

/**
 * <code>FileMessageAdapter</code> provides a wrapper for a file reference. Users
 * can obtain the contents of the message through the payload property and can get
 * the filename and directory in the properties using FileConnector.PROPERTY_FILENAME
 * and FileConnector.PROPERTY_DIRECTORY.<br>
 * This message adaptor supports both InputStream and File payload types.
 */
public class FileMessageAdapter extends AbstractMessageAdapter
{
    /** Serial version */
    private static final long serialVersionUID = 4127485947547548996L;

    protected File file = null;
    protected InputStream fileInputStream;

    public FileMessageAdapter(Object message) throws MessagingException
    {
        super();

        if (message instanceof File)
        {
            this.setFileMessage((File) message);
        }
        else if (message instanceof ReceiverFileInputStream)
        {
            this.setStreamMessage((ReceiverFileInputStream) message);
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, this.getClass());
        }
    }

    protected FileMessageAdapter(FileMessageAdapter template)
    {
        super(template);
        file = template.file;
        fileInputStream = template.fileInputStream;
    }

    public Object getPayload()
    {
        if (fileInputStream != null)
        {
            return fileInputStream;
        }
        return file;
    }

    protected void setFileMessage(File message) throws MessagingException
    {
        this.file = message;
        setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, this.file.getName());
        setProperty(FileConnector.PROPERTY_DIRECTORY, this.file.getParent());
    }

    protected void setStreamMessage(ReceiverFileInputStream message) throws MessagingException
    {
        this.file = message.getCurrentFile();
        this.fileInputStream = message;
        setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, this.file.getName());
        setProperty(FileConnector.PROPERTY_DIRECTORY, this.file.getParent());
    }

    public String getUniqueId()
    {
        return file.getAbsolutePath();
    }

    public ThreadSafeAccess newThreadCopy()
    {
        return new FileMessageAdapter(this);
    }

    /*
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
    }
    */
    
}
