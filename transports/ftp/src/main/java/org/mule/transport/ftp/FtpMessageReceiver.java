/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.retry.RetryContext;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.ConnectException;
import org.mule.transport.file.FileConnector;

import java.io.ByteArrayOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.resource.spi.work.Work;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpMessageReceiver extends AbstractPollingMessageReceiver
{
    protected final FtpConnector connector;
    protected final FilenameFilter filenameFilter;

    // there's nothing like homegrown pseudo-2PC.. :/
    // shared state management like this should go into the connector and use
    // something like commons-tx
    protected final Set<String> scheduledFiles = Collections.synchronizedSet(new HashSet<String>());
    protected final Set<String> currentFiles = Collections.synchronizedSet(new HashSet<String>());

    public FtpMessageReceiver(Connector connector,
                              Service service,
                              InboundEndpoint endpoint,
                              long frequency) throws CreateException
    {
        super(connector, service, endpoint);

        this.setFrequency(frequency);

        this.connector = (FtpConnector) connector;

        if (endpoint.getFilter() instanceof FilenameFilter)
        {
            this.filenameFilter = (FilenameFilter) endpoint.getFilter();
        }
        else
        {
            this.filenameFilter = null;
        }
    }

    public void poll() throws Exception
    {
        FTPFile[] files = listFiles();
        if (logger.isDebugEnabled())
        {
            logger.debug("Poll encountered " + files.length + " new file(s)");
        }

        synchronized (scheduledFiles)
        {
            for (final FTPFile file : files)
            {
                final String fileName = file.getName();

                if (!scheduledFiles.contains(fileName) && !currentFiles.contains(fileName))
                {
                    scheduledFiles.add(fileName);
                    getWorkManager().scheduleWork(new FtpWork(fileName, file));
                }
            }
        }
    }

    protected FTPFile[] listFiles() throws Exception
    {
        FTPClient client = null;
        try
        {
            try
            {
                client = connector.createFtpClient(endpoint);
            }
            catch (Exception e)
            {
                throw new ConnectException(e, this);
            }
            FTPFile[] files = client.listFiles();

            if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
            {
                throw new IOException("Failed to list files. Ftp error: " + client.getReplyCode());
            }

            if (files == null || files.length == 0)
            {
                return files;
            }

            List<FTPFile> v = new ArrayList<FTPFile>();

            for (FTPFile file : files)
            {
                if (file.isFile())
                {
                    if (filenameFilter == null || filenameFilter.accept(null, file.getName()))
                    {
                        v.add(file);
                    }
                }
            }

            return v.toArray(new FTPFile[v.size()]);
        }
        finally
        {
            if (client != null)
            {
                connector.releaseFtp(endpoint.getEndpointURI(), client);
            }
        }
    }

    protected void processFile(FTPFile file) throws Exception
    {
        FTPClient client = null;
        try
        {
            if (!connector.validateFile(file))
            {
                return;
            }
            
            try
            {
                client = connector.createFtpClient(endpoint);
            }
            catch (Exception e)
            {
                throw new ConnectException(e, this);
            }

            MuleMessage message;
            if (connector.isStreaming())
            {
                InputStream stream = client.retrieveFileStream(file.getName());
                if (stream == null)
                {
                    throw new IOException(MessageFormat.format("Failed to retrieve file {0}. Ftp error: {1}",
                                                               file.getName(), client.getReplyCode()));
                }
                message = new DefaultMuleMessage(connector.getMessageAdapter(stream));
            }
            else
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (!client.retrieveFile(file.getName(), baos))
                {
                    throw new IOException(MessageFormat.format("Failed to retrieve file {0}. Ftp error: {1}",
                                                               file.getName(), client.getReplyCode()));
                }
                byte[] bytes = baos.toByteArray();
                if (bytes.length > 0)
                {
                    message = new DefaultMuleMessage(connector.getMessageAdapter(bytes));            
                }
                else
                {
                    throw new IOException("File " + file.getName() + " is empty (zero bytes)");
                }
            }

            message.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, file.getName());
            message.setProperty(FileConnector.PROPERTY_FILE_SIZE, file.getSize());
            routeMessage(message);

            postProcess(client, file, message);
        }
        finally
        {
            if (client != null)
            {
                connector.releaseFtp(endpoint.getEndpointURI(), client);
            }
        }
    }

    protected void postProcess(FTPClient client, FTPFile file, MuleMessage message) throws Exception
    {
        if (!client.deleteFile(file.getName()))
        {
            throw new IOException(MessageFormat.format("Failed to delete file {0}. Ftp error: {1}",
                                                       file.getName(), client.getReplyCode()));
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Deleted processed file " + file.getName());
        }
        
        // MULE-4372 This causes the receiver to hang indefinitely
        //if (!client.completePendingCommand())
        //{
        //    throw new IOException(MessageFormat.format("Failed to complete a pending command. Retrieveing file {0}. Ftp error: {1}",
        //                                               file.getName(), client.getReplyCode()));
        //}
    }
    
    protected void doConnect() throws Exception
    {
        // no op
    }

    @Override
    public RetryContext validateConnection(RetryContext retryContext)
    {
        FTPClient client = null;
        try
        {
            client = connector.createFtpClient(endpoint);
            client.sendNoOp();
            client.logout();
            client.disconnect();

            retryContext.setOk();
        }
        catch (Exception ex)
        {
            retryContext.setFailed(ex);
        }
        finally
        {
            try
            {
                if (client != null)
                {
                    connector.releaseFtp(endpoint.getEndpointURI(), client);
                }
            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Failed to release ftp client " + client, e);
                }
            }
        }

        return retryContext;
    }
        
    protected void doDisconnect() throws Exception
    {
        // no op
    }

    protected void doDispose()
    {
        // template method
    }

    private final class FtpWork implements Work
    {
        private final String name;
        private final FTPFile file;

        private FtpWork(String name, FTPFile file)
        {
            this.name = name;
            this.file = file;
        }

        public void run()
        {
            try
            {
                currentFiles.add(name);
                processFile(file);
            }
            catch (Exception e)
            {
                connector.handleException(e);
            }
            finally
            {
                currentFiles.remove(name);
                scheduledFiles.remove(name);
            }
        }

        public void release()
        {
            // no op
        }
    }

}
