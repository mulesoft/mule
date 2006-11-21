/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import java.io.File;

import org.apache.commons.lang.ObjectUtils;
import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.file.transformers.FileToByteArray;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

/**
 * <code>FileMessageAdapter</code> provides a wrapper for a file reference. Users
 * can obtain the contents of the message through the payload property and can get
 * the filename and directory in the properties using PROPERTY_FILENAME and
 * PROPERTY_DIRECTORY.
 */
public class FileMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4127485947547548996L;

    private static final FileToByteArray transformer = new FileToByteArray();

    private File file = null;
    private byte[] contents = null;

    public FileMessageAdapter(Object message) throws MessagingException
    {
        super();

        if (message instanceof File)
        {
            this.setMessage((File)message);
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, this.getClass());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getPayload()
     */
    public Object getPayload()
    {
        return file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        synchronized (this)
        {
            if (contents == null)
            {
                try
                {
                    // TODO unfortunately reading the file here is required,
                    // since otherwise the FileMessageReceiver might delete the
                    // file
                    this.contents = (byte[])transformer.transform(file);
                }
                catch (Exception noPayloadException)
                {
                    throw new MuleException(new Message(Messages.FAILED_TO_READ_PAYLOAD), noPayloadException);
                }
            }
            return contents;
        }
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        synchronized (this)
        {
            return new String(this.getPayloadAsBytes(), encoding);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#setMessage(java.lang.Object)
     */
    protected void setMessage(File message) throws MessagingException
    {
        boolean fileIsValid;
        Exception fileInvalidException;

        try
        {
            fileIsValid = (message != null && message.isFile());
            fileInvalidException = null;
        }
        catch (Exception ex)
        {
            // save any file access exceptions
            fileInvalidException = ex;
            fileIsValid = false;
        }

        if (!fileIsValid)
        {
            Object exceptionArg;

            if (fileInvalidException != null)
            {
                exceptionArg = fileInvalidException;
            }
            else
            {
                exceptionArg = ObjectUtils.toString(message, "null");
            }

            Message msg = new Message(Messages.FILE_X_DOES_NOT_EXIST, ObjectUtils.toString(message, "null"));

            throw new MessagingException(msg, exceptionArg);
        }

        this.file = message;
        this.contents = null;
        this.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, this.file.getName());
        this.setProperty(FileConnector.PROPERTY_DIRECTORY, this.file.getParent());
    }

    public String getUniqueId()
    {
        return file.getAbsolutePath();
    }

}
