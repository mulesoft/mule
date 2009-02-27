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

import org.mule.DefaultMuleMessage;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.file.i18n.FileMessages;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;

/**
 * <code>FileMessageDispatcher</code> is used to read/write files to the filesystem
 */
public class FileMessageDispatcher extends AbstractMessageDispatcher
{
    private final FileConnector connector;

    public FileMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (FileConnector) endpoint.getConnector();

        if (endpoint.getProperty("outputAppend") != null)
        {
            throw new IllegalArgumentException("Configuring 'outputAppend' on a file endpoint is no longer supported. You may configure it on a file connector instead.");
        }
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        Object data = event.transformMessage();
        // Wrap the transformed message before passing it to the filename parser
        MuleMessage message = new DefaultMuleMessage(data, event.getMessage());

        FileOutputStream fos = (FileOutputStream) connector.getOutputStream(event.getEndpoint(), message);
        try
        {
            if (event.getMessage().getStringProperty(FileConnector.PROPERTY_FILENAME, null) == null)
            {
                event.getMessage().setStringProperty(FileConnector.PROPERTY_FILENAME,
                        message.getStringProperty(FileConnector.PROPERTY_FILENAME, ""));
            }

            if (data instanceof byte[])
            {
                fos.write((byte[]) data);
            }
            else if (data instanceof String)
            {
                fos.write(data.toString().getBytes(event.getEncoding()));
            }
            else if (data instanceof OutputHandler)
            {
                ((OutputHandler) data).write(event, fos);
            }
            else
            {
                InputStream is = (InputStream) event.transformMessage(InputStream.class);
                IOUtils.copyLarge(is, fos);
                is.close();
            }
        }
        finally
        {
            logger.debug("Closing file");
            fos.close();
        }
    }

    /**
     * There is no associated session for a file connector
     *
     * @throws MuleException
     */
    public Object getDelegateSession() throws MuleException
    {
        return null;
    }

    protected static File getNextFile(String dir, FilenameFilter filter) throws MuleException
    {
        File[] files;
        File file = FileUtils.newFile(dir);
        File result = null;
        try
        {
            if (file.exists())
            {
                if (file.isFile())
                {
                    result = file;
                }
                else if (file.isDirectory())
                {
                    if (filter != null)
                    {
                        files = file.listFiles(filter);
                    }
                    else
                    {
                        files = file.listFiles();
                    }
                    if (files.length > 0)
                    {
                        result = files[0];
                    }
                }
            }
            return result;
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(FileMessages.errorWhileListingFiles(), e);
        }
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        return null;
    }

    protected void doDispose()
    {
        // no op
    }

    protected void doConnect() throws Exception
    {
        // no op
    }

    protected void doDisconnect() throws Exception
    {
        // no op
    }

}
