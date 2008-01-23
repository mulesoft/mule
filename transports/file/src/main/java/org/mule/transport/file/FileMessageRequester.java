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
import org.mule.RegistryContext;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.file.filters.FilenameWildcardFilter;
import org.mule.transport.file.i18n.FileMessages;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URLDecoder;

/**
 * <code>FileMessageDispatcher</code> is used to read/write files to the filesystem
 */
public class FileMessageRequester extends AbstractMessageRequester
{
    private final FileConnector connector;

    public FileMessageRequester(ImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (FileConnector) endpoint.getConnector();
    }

    /**
     * There is no associated session for a file connector
     *
     * @throws org.mule.api.MuleException
     */
    public Object getDelegateSession() throws MuleException
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

    protected MuleMessage doRequest(long timeout) throws Exception
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
                result = FileMessageDispatcher.getNextFile(endpoint.getEndpointURI().getAddress(), filenameFilter);
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

                DefaultMuleMessage message;
                File destinationFile = null;
                if (connector.getMoveToDirectory() != null)
                {
                    destinationFile = FileUtils.newFile(connector.getMoveToDirectory(), result
                        .getName());
                    if (!result.renameTo(destinationFile))
                    {
                        logger.error("Failed to move file: " + result.getAbsolutePath()
                                     + " to " + destinationFile.getAbsolutePath());
                        message = new DefaultMuleMessage(connector.getMessageAdapter(result));
                    }
                    else
                    {
                        message = new DefaultMuleMessage(connector.getMessageAdapter(destinationFile));
                    }

                }
                else
                {
                    message = new DefaultMuleMessage(connector.getMessageAdapter(result));
                }

                if (connector.isAutoDelete())
                {
                    // no moveTo directory
                    if (destinationFile == null)
                    {
                        // delete source
                        if (!result.delete())
                        {
                            throw new DefaultMuleException(
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