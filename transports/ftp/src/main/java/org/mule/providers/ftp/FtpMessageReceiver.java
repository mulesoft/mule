/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractPollingMessageReceiver;
import org.mule.providers.file.FileConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;

import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.resource.spi.work.Work;

import org.apache.commons.io.output.ByteArrayOutputStream;
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
    protected final Set scheduledFiles = Collections.synchronizedSet(new HashSet());
    protected final Set currentFiles = Collections.synchronizedSet(new HashSet());

    public FtpMessageReceiver(UMOConnector connector,
                              UMOComponent component,
                              UMOEndpoint endpoint,
                              long frequency) throws CreateException
    {
        super(connector, component, endpoint);

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

        synchronized (scheduledFiles)
        {
            for (int i = 0; i < files.length; i++)
            {
                final FTPFile file = files[i];
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
        final UMOEndpointURI uri = endpoint.getEndpointURI();
        FTPClient client = connector.getFtp(uri);

        try
        {
            connector.enterActiveOrPassiveMode(client, endpoint);
            connector.setupFileType(client, endpoint);

            final String path = uri.getPath();
            if (!client.changeWorkingDirectory(path))
            {
                throw new IOException(MessageFormat.format("Failed to change working directory to {0}. Ftp error: {1}",
                        new Object[]{path, new Integer(client.getReplyCode())}));
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

            List v = new ArrayList();

            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isFile())
                {
                    if (filenameFilter == null || filenameFilter.accept(null, files[i].getName()))
                    {
                        v.add(files[i]);
                    }
                }
            }

            return (FTPFile[]) v.toArray(new FTPFile[v.size()]);
        }
        finally
        {
            connector.releaseFtp(uri, client);
        }
    }

    protected void processFile(FTPFile file) throws Exception
    {
        logger.debug("entering processFile()");
        UMOEndpointURI uri = endpoint.getEndpointURI();
        FTPClient client = connector.getFtp(uri);

        try
        {
            connector.enterActiveOrPassiveMode(client, endpoint);
            connector.setupFileType(client, endpoint);

            final String fileName = file.getName();
            final String path = uri.getPath();

            if (!client.changeWorkingDirectory(path))
            {
                throw new IOException(MessageFormat.format("Failed to change working directory to {0}. Ftp error: {1}",
                        new Object[]{path, new Integer(client.getReplyCode())}));
            }

            UMOMessage message;
            if (endpoint.isStreaming())
            {
                message = new MuleMessage(
                        connector.getStreamMessageAdapter(client.retrieveFileStream(fileName), null));
            }
            else
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (!client.retrieveFile(fileName, baos))
                {
                    throw new IOException(MessageFormat.format("Failed to retrieve file {0}. Ftp error: {1}",
                            new Object[]{fileName, new Integer(client.getReplyCode())}));
                }
                message = new MuleMessage(connector.getMessageAdapter(baos.toByteArray()));
            }

            message.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, fileName);
            routeMessage(message);

            if (!client.deleteFile(fileName))
            {
                throw new IOException(MessageFormat.format("Failed to delete file {0}. Ftp error: {1}",
                        new Object[]{fileName, new Integer(client.getReplyCode())}));
            }
        }
        finally
        {
            logger.debug("leaving processFile()");
            connector.releaseFtp(uri, client);
        }
    }

    protected void doConnect() throws Exception
    {
        // why?!
        //connector.releaseFtp(getEndpointURI());
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
