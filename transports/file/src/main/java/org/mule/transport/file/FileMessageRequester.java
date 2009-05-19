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
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.transport.file.i18n.FileMessages;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

/**
 * <code>FileMessageDispatcher</code> is used to read/write files to the filesystem
 */
public class FileMessageRequester extends AbstractMessageRequester
{
    private final FileConnector connector;

    private FilenameFilter filenameFilter = null;
    private FileFilter fileFilter = null;
    
    public FileMessageRequester(InboundEndpoint endpoint) throws MuleException
    {
        super(endpoint);
        this.connector = (FileConnector) endpoint.getConnector();
        
        Filter filter = endpoint.getFilter();
        if (filter instanceof FilenameFilter)
        {
            filenameFilter = (FilenameFilter) filter;
        }
        else if (filter instanceof FileFilter)
        {
            fileFilter = (FileFilter) filter;
        }
        else if (filter != null)
        {
            throw new CreateException(FileMessages.invalidFileFilter(endpoint.getEndpointURI()), this);
        }
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
     * 
     * @param timeout this is ignored when doing a receive on this dispatcher
     * @return a message containing file contents or null if there was notthing to
     *         receive
     * @throws Exception
     */

    protected MuleMessage doRequest(long timeout) throws Exception
    {
        File file = FileUtils.newFile(endpoint.getEndpointURI().getAddress());
        File result = null;
        
        if (file.exists())
        {
            if (file.isFile())
            {
                result = file;
            }
            else if (file.isDirectory())
            {
                if (fileFilter != null)
                {
                    result = FileMessageDispatcher.getNextFile(
                        endpoint.getEndpointURI().getAddress(), fileFilter);
                }
                else
                {
                    result = FileMessageDispatcher.getNextFile(
                        endpoint.getEndpointURI().getAddress(), filenameFilter);
                }
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
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("The file has not aged enough yet, will return nothing for: "
                                         + result.getCanonicalPath());
                        }
                        return null;
                    }
                }

                // Don't we need to try to obtain a file lock as we do with receiver
                String sourceFileOriginalName = result.getName();

                // This isn't nice but is needed as MessageAdaptor is required to
                // resolve
                // destination file name, and StreamingReceiverFileInputStream is
                // required to create MessageAdaptor
                DefaultMessageAdapter fileParserMsgAdaptor = new DefaultMessageAdapter(null);
                fileParserMsgAdaptor.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, sourceFileOriginalName);

                // set up destination file
                File destinationFile = null;
                String movDir = connector.getMoveToDirectory();
                if (movDir != null)
                {
                    String destinationFileName = sourceFileOriginalName;
                    String moveToPattern = connector.getMoveToPattern();
                    if (moveToPattern != null)
                    {
                        destinationFileName = connector.getFilenameParser().getFilename(
                            fileParserMsgAdaptor, moveToPattern);
                    }
                    // don't use new File() directly, see MULE-1112
                    destinationFile = FileUtils.newFile(movDir, destinationFileName);
                }

                MessageAdapter msgAdapter = null;
                try
                {
                    if (connector.isStreaming())
                    {
                        msgAdapter = connector.getMessageAdapter(new ReceiverFileInputStream(result, connector.isAutoDelete(),
                            destinationFile));
                    }
                    else
                    {
                        msgAdapter = connector.getMessageAdapter(result);
                    }
                }
                catch (FileNotFoundException e)
                {
                    // we can ignore since we did manage to acquire a lock, but just
                    // in case
                    logger.error("File being read disappeared!", e);
                    return null;
                }
                msgAdapter.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, sourceFileOriginalName);

                if (!connector.isStreaming())
                {
                    moveOrDelete(result, destinationFile);
                    return new DefaultMuleMessage(msgAdapter);
                }
                else
                {
                    // If we are streaming no need to move/delete now, that will be
                    // done when stream is closed
                    return new DefaultMuleMessage(msgAdapter);
                }
            }
        }
        return null;
    }

    private void moveOrDelete(final File sourceFile, File destinationFile) throws DefaultMuleException
    {

        if (destinationFile != null)
        {
            // move sourceFile to new destination
            if (!FileUtils.moveFile(sourceFile, destinationFile))
            {
                throw new DefaultMuleException(FileMessages.failedToMoveFile(sourceFile.getAbsolutePath(),
                    destinationFile.getAbsolutePath()));
            }
        }
        if (connector.isAutoDelete())
        {
            // no moveTo directory
            if (destinationFile == null)
            {
                // delete source
                if (!sourceFile.delete())
                {
                    throw new DefaultMuleException(FileMessages.failedToDeleteFile(sourceFile));
                }
            }
            else
            {
                // nothing to do here since moveFile() should have deleted
                // the source file for us
            }
        }

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
