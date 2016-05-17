/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;
import org.mule.transport.file.i18n.FileMessages;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileFilter;
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

    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        Object data = event.getMessage().getPayload();
        // Wrap the transformed message before passing it to the filename parser
        MuleMessage message = new DefaultMuleMessage(data, event.getMessage(), event.getMuleContext());

        FileOutputStream fos = (FileOutputStream) connector.getOutputStream(getEndpoint(), event);
        try
        {
            if (event.getMessage().getOutboundProperty(FileConnector.PROPERTY_FILENAME) == null)
            {
                event.getMessage().setOutboundProperty(FileConnector.PROPERTY_FILENAME,
                                                       message.getOutboundProperty(FileConnector.PROPERTY_FILENAME,
                                                                                   StringUtils.EMPTY));
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
                InputStream is = event.transformMessage(DataTypeFactory.create(InputStream.class));
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

    protected static File getNextFile(String dir, Object filter) throws MuleException
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
                        if (filter instanceof FileFilter)
                        {
                            files = file.listFiles((FileFilter) filter);
                        }
                        else if (filter instanceof FilenameFilter)
                        {
                            files = file.listFiles((FilenameFilter) filter);
                        }
                        else
                        {
                            throw new DefaultMuleException(FileMessages.invalidFilter(filter));
                        }
                    }
                    else
                    {
                        files = file.listFiles();
                    }
                    if (files.length > 0)
                    {
                        result = getFirstFile(files);
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

    private static File getFirstFile(File[] files)
    {
        for (File file : files)
        {
            if (file.isFile())
            {
                return  file;
            }
        }

        return null;
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        return new DefaultMuleMessage(NullPayload.getInstance(), getEndpoint().getMuleContext());
    }

    @Override
    protected void doDispose()
    {
        // no op
    }

    @Override
    protected void doConnect() throws Exception
    {
        // no op
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // no op
    }
}
