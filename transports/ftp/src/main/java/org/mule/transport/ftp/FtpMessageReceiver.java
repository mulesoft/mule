/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.retry.RetryContext;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transport.Connector;
import org.mule.transaction.TransactionTemplate;
import org.mule.transaction.TransactionTemplateFactory;
import org.mule.transport.AbstractPollingMessageReceiver;

import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.resource.spi.work.Work;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.FTPReply;

public class FtpMessageReceiver extends AbstractPollingMessageReceiver
{
    private final static int FTP_LIST_PAGE_SIZE = 25;
    protected final FtpConnector connector;
    protected final FilenameFilter filenameFilter;

    // there's nothing like homegrown pseudo-2PC.. :/
    // shared state management like this should go into the connector and use
    // something like commons-tx
    protected final Set<String> scheduledFiles = Collections.synchronizedSet(new HashSet<String>());
    protected final Set<String> currentFiles = Collections.synchronizedSet(new HashSet<String>());

    public FtpMessageReceiver(Connector connector,
                              FlowConstruct flowConstruct,
                              InboundEndpoint endpoint,
                              long frequency) throws CreateException
    {
        super(connector, flowConstruct, endpoint);

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

    @Override
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
                if (getLifecycleState().isStopping())
                {
                    break;
                }

                final String fileName = file.getName();

                if (!scheduledFiles.contains(fileName) && !currentFiles.contains(fileName))
                {
                    scheduledFiles.add(fileName);
                    getWorkManager().scheduleWork(new FtpWork(fileName, file));
                }
            }
        }
    }

    @Override
    protected boolean pollOnPrimaryInstanceOnly()
    {
        return true;
    }

    protected FTPFile[] listFiles() throws Exception
    {
        FTPClient client = null;
        try
        {
            client = connector.createFtpClient(endpoint);
            FTPListParseEngine engine = client.initiateListParsing();
            FTPFile[] files = null;
            List<FTPFile> v = new ArrayList<FTPFile>();
            while (engine.hasNext())
            {
                if (getLifecycleState().isStopping())
                {
                    break;
                }
                files = engine.getNext(FTP_LIST_PAGE_SIZE);
                if (files == null || files.length == 0)
                {
                    return files;
                }
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
            }

            if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
            {
                throw new IOException("Failed to list files. Ftp error: " + client.getReplyCode());
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
        factory.setStreaming(connector.isStreaming());
        factory.setFtpClient(client);    
        
        return factory;
    }

    protected void postProcess(FTPClient client, FTPFile file) throws Exception
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
        
        if (connector.isStreaming())
        {
            if (!client.completePendingCommand())
            {
                throw new IOException(MessageFormat.format("Failed to complete a pending command. Retrieveing file {0}. Ftp error: {1}",
                                                           file.getName(), client.getReplyCode()));
            }
        }
    }
    
    @Override
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
        
    @Override
    protected void doDisconnect() throws Exception
    {
        // no op
    }

    @Override
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
            FTPClient client = null;
            try
            {
                client = connector.createFtpClient(endpoint);
                final FTPClient finalClient = client;
                TransactionTemplate<Void> exceptionHandlingTransactionTemplate = TransactionTemplateFactory.<Void>createExceptionHandlingTransactionTemplate(getConnector().getMuleContext());
                exceptionHandlingTransactionTemplate.execute(new TransactionCallback<Void>()
                {
                    @Override
                    public Void doInTransaction() throws Exception
                    {
                        currentFiles.add(name);

                        MuleMessage message;
                        if (!connector.validateFile(file))
                        {
                            return null;
                        }

                        FtpMuleMessageFactory muleMessageFactory = createMuleMessageFactory(finalClient);
                        message = muleMessageFactory.create(file, endpoint.getEncoding());
                        routeMessage(message);
                        postProcess(finalClient, file);
                        return null;
                    }
                });
            }
            catch (MessagingException e)
            {
                //Already handled by TransactionTemplate
                if (!e.isCauseRollback())
                {
                    try
                    {
                        postProcess(client,file);
                    }
                    catch (Exception e1)
                    {
                        logger.error(e);
                    }
                }
            }
            catch (Exception e)
            {
                getConnector().getMuleContext().getExceptionListener().handleException(e);
            }
            finally
            {
                if (client != null)
                {
                    try
                    {
                        connector.releaseFtp(endpoint.getEndpointURI(), client);
                    }
                    catch (Exception e)
                    {
                        logger.error(e);
                    }
                }
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
