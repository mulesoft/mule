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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URLDecoder;

import org.mule.MuleException;
import org.mule.RegistryContext;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.file.filters.FilenameWildcardFilter;
import org.mule.providers.file.i18n.FileMessages;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.OutputHandler;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

/**
 * <code>FileMessageDispatcher</code> is used to read/write files to the filesystem
 */
public class FileMessageDispatcher extends AbstractMessageDispatcher
{
    private final FileConnector connector;

    public FileMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (FileConnector) endpoint.getConnector();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#dispatch(org.mule.umo.UMOEvent)
     */
    protected void doDispatch(UMOEvent event) throws Exception
    {
        Object data = event.getTransformedMessage();
        // Wrap the transformed message before passing it to the filename parser
        UMOMessage message = new MuleMessage(data, event.getMessage());

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
                InputStream is = (InputStream) event.getTransformedMessage(InputStream.class);
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
     * @throws UMOException
     */
    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    /**
     * Will attempt to do a receive from a directory, if the endpointUri resolves to
     * a file name the file will be returned, otherwise the first file in the
     * directory according to the filename filter configured on the connector.
     * @param timeout this is ignored when doing a receive on this dispatcher
     * @return a message containing file contents or null if there was notthing to
     *         receive
     * @throws Exception
     */

    protected UMOMessage doReceive(long timeout) throws Exception
    {
        File file = FileUtils.newFile(endpoint.getEndpointURI().getAddress());
        File result = null;
        FilenameFilter filenameFilter = null;
        String filter = (String) endpoint.getProperty("filter");
        if (filter != null)
        {
            filter = URLDecoder.decode(filter, RegistryContext.getConfiguration().getDefaultEncoding());
            filenameFilter = new FilenameWildcardFilter(filter);
        }
        if (file.exists())
        {
            if (file.isFile())
            {
                result = file;
            }
            else if (file.isDirectory())
            {
                result = getNextFile(endpoint.getEndpointURI().getAddress(), filenameFilter);
            }
            if (result != null)
            {
                boolean checkFileAge = connector.getCheckFileAge();
                if (checkFileAge)
                {
                    long fileAge = connector.getFileAge();
                    long lastMod = result.lastModified();
                    long now = System.currentTimeMillis();
                    long thisFileAge = now - lastMod;
                    if (thisFileAge < fileAge)
                    {
                        if (logger.isDebugEnabled()) {
                            logger.debug("The file has not aged enough yet, will return nothing for: " +
                                         result.getCanonicalPath());
                        }
                        return null;
                    }
                }

                MuleMessage message = new MuleMessage(connector.getMessageAdapter(result));
                File destinationFile = null;
                if (connector.getMoveToDirectory() != null)
                {
                    destinationFile = FileUtils.newFile(connector.getMoveToDirectory(), result
                        .getName());
                    if (!result.renameTo(destinationFile))
                    {
                        logger.error("Failed to move file: " + result.getAbsolutePath()
                                     + " to " + destinationFile.getAbsolutePath());
                    }
                }
                
                if (connector.isAutoDelete())
                {
                    // no moveTo directory
                    if (destinationFile == null)
                    {
                        // delete source
                        if (!result.delete())
                        {
                            throw new MuleException(
                                FileMessages.failedToDeleteFile(result.getAbsolutePath()));
                        }
                    }

                    // nothing to do here since moveFile() should have deleted
                    // the source file for us
                }
                
                return message;
            }
        }
        return null;
    }

    private File getNextFile(String dir, FilenameFilter filter) throws UMOException
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
            throw new MuleException(FileMessages.errorWhileListingFiles(), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOConnectorSession#send(org.mule.umo.UMOEvent)
     */
    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        doDispatch(event);
        return event.getMessage();
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
