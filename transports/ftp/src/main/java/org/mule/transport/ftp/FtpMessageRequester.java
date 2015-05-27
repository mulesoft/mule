/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.AbstractMessageRequester;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.FTPReply;

public class FtpMessageRequester extends AbstractMessageRequester
{
    private final static int FTP_LIST_PAGE_SIZE = 25;
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
    @Override
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        FTPClient client = null;
        try
        {
            client = connector.createFtpClient(endpoint);
            FTPFile fileToProcess;
            if(connector.isFile(endpoint,client))
            {
                fileToProcess = client.listFiles(endpoint.getEndpointURI().getPath())[0];
                if(!isValid(fileToProcess, getFilenameFilter()))
                {
                    return null;
                }
            } 
            else
            {
                fileToProcess = findFileToProcess(client);
                if (fileToProcess == null)
                {
                    return null;
                }
            }

            fileToProcess = prepareFile(client, fileToProcess);

            FtpMuleMessageFactory messageFactory = createMuleMessageFactory(client);
            MuleMessage message = messageFactory.create(fileToProcess, endpoint.getEncoding(), endpoint.getMuleContext());
            postProcess(client, fileToProcess, message);
            return message;
        }
        finally
        {
            connector.releaseFtp(endpoint.getEndpointURI(), client);
        }
    }
    
    protected void postProcess(FTPClient client, FTPFile file, MuleMessage message) throws Exception
    {
        if (!connector.isStreaming())
        {
            if (!client.deleteFile(file.getName()))
            {
                throw new IOException(MessageFormat.format("Failed to delete file {0}. Ftp error: {1}", file.getName(), client.getReplyCode()));
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Deleted file " + file.getName());
            }
        }
    }

    @Override
    protected void initializeMessageFactory() throws InitialisationException
    {
        // Do not initialize the muleMessageFactory instance variable of our super class as 
        // we're creating MuleMessageFactory instances per request. 
        // See createMuleMessageFactory(FTPClient) below.
    }

    protected FtpMuleMessageFactory createMuleMessageFactory(FTPClient client) throws CreateException
    {
        FtpMuleMessageFactory factory = (FtpMuleMessageFactory) createMuleMessageFactory();
        // We might want to use isStreaming from connector, but for now maintain existing behaviour.
        factory.setStreaming(false);
        factory.setFtpClient(client);
        
        return factory;
    }

    protected FTPFile prepareFile(FTPClient client, FTPFile file) throws IOException
    {
        return file;
    }

    protected FTPFile findFileToProcess(FTPClient client) throws Exception
    {
        //Checking if it is a file or a directory
        boolean isFile = connector.isFile(endpoint, client);
        FTPListParseEngine engine = client.initiateListParsing();
        FTPFile[] files = null;
        while (engine.hasNext())
        {
            files = engine.getNext(FTP_LIST_PAGE_SIZE);
            if (files == null)
            {
                break;
            }
            FilenameFilter filenameFilter = getFilenameFilter();
            for (int i = 0; i < files.length; i++)
            {
                FTPFile file = files[i];
                if (file.isFile() && isValid(file, filenameFilter))
                {
                    // only read the first one
                    return file;
                }
            }
        }
        if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
        {
            throw new IOException("Ftp error: " + client.getReplyCode());
        }
        
        return null;
    }

    protected boolean isValid(FTPFile file, FilenameFilter filenameFilter)
    {
        return filenameFilter.accept(null, file.getName()) && connector.validateFile(file);
    }

    protected FTPFile[] listFiles(FTPClient client) throws IOException
    {
        // no longer used, only kept to preserve the class protected API
        return null;
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
        public AcceptAllFilenameFilter()
        {
            super();
        }
        
        public boolean accept(File dir, String name)
        {
            return true;
        }
    }
}
