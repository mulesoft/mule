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
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.file.FileConnector;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpMessageRequester extends AbstractMessageRequester
{
    protected final FtpConnector connector;

    public FtpMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (FtpConnector) endpoint.getConnector();
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
        try
        {
            EndpointURI uri = endpoint.getEndpointURI();
            FTPClient client = connector.getFtp(uri);
            connector.destroyFtp(uri, client);
        }
        catch (Exception e)
        {
            // pool may be closed
        }
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout The maximum time the operation should block before returning.
     *         The call should return immediately if there is data available. If
     *         no data becomes available before the timeout elapses, null will be
     *         returned.
     * @return The result of the request wrapped in a MuleMessage object. <code>null</code> will be
     *          returned if no data was avaialable
     * @throws Exception if the call to the underlying protocol cuases an exception
     */
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        FTPClient client = null;
        try
        {
            client = connector.createFtpClient(endpoint);
            FTPFile fileToProcess = findFileToProcess(client);
            
            String originalFileName = fileToProcess.getName();
            fileToProcess = prepareFile(client, fileToProcess);
            
            byte[] payload = retriveFileContents(client, fileToProcess);
            
            MuleMessage reply = new DefaultMuleMessage(connector.getMessageAdapter(payload), 
                connector.getMuleContext());
            reply.setProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, originalFileName);
            reply.setProperty(FileConnector.PROPERTY_FILE_SIZE, new Long(fileToProcess.getSize()));
            return reply;
        }
        finally
        {
            connector.releaseFtp(endpoint.getEndpointURI(), client);
        }
    }

    protected FTPFile prepareFile(FTPClient client, FTPFile file) throws IOException
    {
        return file;
    }

    private byte[] retriveFileContents(FTPClient client, FTPFile fileToProcess) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!client.retrieveFile(fileToProcess.getName(), baos))
        {
            throw new IOException("Ftp error: " + client.getReplyCode());
        }
        return baos.toByteArray();
    }

    protected FTPFile findFileToProcess(FTPClient client) throws Exception
    {
        FTPFile[] files = listFiles(client);

        FilenameFilter filenameFilter = getFilenameFilter();
        for (int i = 0; i < files.length; i++)
        {
            FTPFile file = files[i];
            if (file.isFile())
            {
                if (filenameFilter.accept(null, file.getName()))
                {
                    if (connector.validateFile(file))
                    {
                        // only read the first one
                        return file;
                    }
                }
            }
        }
        
        return null;
    }

    protected FTPFile[] listFiles(FTPClient client) throws IOException
    {
        FTPFile[] files = client.listFiles();
        if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
        {
            throw new IOException("Ftp error: " + client.getReplyCode());
        }
        
        if (files == null || files.length == 0)
        {
            return null;
        }
        
        return files;
    }

    protected FilenameFilter getFilenameFilter()
    {
        if (endpoint.getFilter() instanceof FilenameFilter)
        {
            return (FilenameFilter) endpoint.getFilter();
        }
        
        return new AcceptAllFilenameFilter();
    }
    
    private static class AcceptAllFilenameFilter implements FilenameFilter
    {
        public boolean accept(File dir, String name)
        {
            return true;
        }
    }
}
