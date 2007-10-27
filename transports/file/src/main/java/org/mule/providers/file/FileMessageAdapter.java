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

import org.mule.impl.ThreadSafeAccess;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.file.i18n.FileMessages;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>FileMessageAdapter</code> provides a wrapper for a file reference. Users
 * can obtain the contents of the message through the payload property and can get
 * the filename and directory in the properties using FileConnector.PROPERTY_FILENAME and
 * FileConnector.PROPERTY_DIRECTORY.
 */
public class FileMessageAdapter extends AbstractMessageAdapter
{
    /** Serial version */
    private static final long serialVersionUID = 4127485947547548996L;

    protected File file = null;
    protected InputStream payload;

    public FileMessageAdapter(Object message) throws MessagingException
    {
        super();

        if (message instanceof File)
        {
            this.setFileMessage((File) message);
        }
        else if(message instanceof FileMessageReceiver.ReceiverFileInputStream)
        {
            this.setStreamMessage((FileMessageReceiver.ReceiverFileInputStream)message);
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
        payload = template.payload;
    }

    public Object getPayload()
    {
        return file;
    }

    protected void setFileMessage(File message) throws MessagingException
    {
        try
        {
            this.file = message;
            this.payload = new FileInputStream(message);
        }
        catch (IOException ex)
        {
            throw new MessagingException(FileMessages.fileDoesNotExist(ObjectUtils.toString(message, "null")), ex);
        }
        setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, this.file.getName());
        setProperty(FileConnector.PROPERTY_DIRECTORY, this.file.getParent());
    }

    protected void setStreamMessage(FileMessageReceiver.ReceiverFileInputStream message) throws MessagingException
    {
        this.file = message.getCurrentFile();
        this.payload = message;
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

}
