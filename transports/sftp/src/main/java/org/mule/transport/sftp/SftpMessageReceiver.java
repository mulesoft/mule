/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.transport.Connector;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.ConnectException;
import org.mule.transport.sftp.notification.SftpNotifier;
import org.mule.util.ValueHolder;
import org.mule.util.lock.LockFactory;

import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * <code>SftpMessageReceiver</code> polls and receives files from an sftp service
 * using jsch. This receiver produces an InputStream payload, which can be
 * materialized in a MessageDispatcher or Component.
 */
public class SftpMessageReceiver extends AbstractPollingMessageReceiver
{

    private SftpReceiverRequesterUtil sftpRRUtil = null;
    private LockFactory lockFactory;
    private boolean poolOnPrimaryInstanceOnly;
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    public SftpMessageReceiver(SftpConnector connector,
                               FlowConstruct flow,
                               InboundEndpoint endpoint,
                               long frequency) throws CreateException
    {
        super(connector, flow, endpoint);

        this.setFrequency(frequency);

        sftpRRUtil = createSftpReceiverRequesterUtil(endpoint);
    }

    protected SftpReceiverRequesterUtil createSftpReceiverRequesterUtil(InboundEndpoint endpoint)
    {
        return new SftpReceiverRequesterUtil(endpoint);
    }

    public SftpMessageReceiver(SftpConnector connector, FlowConstruct flow, InboundEndpoint endpoint) throws CreateException
    {
        this(connector, flow, endpoint, DEFAULT_POLL_FREQUENCY);
    }

    public void poll() throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Polling. Called at endpoint " + endpoint.getEndpointURI());
        }
        try
        {
            if (!connected.get() )
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Skipping poll since message receiver is not yet connected");
                }
                return;
            }
            String[] files;
            try
            {
                files = sftpRRUtil.getAvailableFiles(false);
            }
            catch (Exception e)
            {
                connected.set(false);
                throw new ConnectException(e, this);
            }

            if (files.length == 0)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Polling. No matching files found at endpoint " + endpoint.getEndpointURI());
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Polling. " + files.length + " files found at " + endpoint.getEndpointURI()
                                 + ":" + Arrays.toString(files));
                }
                for (String file : files)
                {
                    if (getLifecycleState().isStopping())
                    {
                        break;
                    }
                    Lock fileLock = lockFactory.createLock(createLockId(file));
                    if (fileLock.tryLock(10, TimeUnit.MILLISECONDS))
                    {
                        try
                        {
                            routeFile(file);
                        }
                        finally
                        {
                            fileLock.unlock();
                        }
                    }
                }
                if (logger.isDebugEnabled())
                {
                    logger.debug("Polling. Routed all " + files.length + " files found at "
                                 + endpoint.getEndpointURI());
                }
            }
        }
        catch (MessagingException e)
        {
            //Already handled by TransactionTemplate
        }
        catch (Exception e)
        {
            logger.error("Error in poll", e);
            getEndpoint().getMuleContext().getExceptionListener().handleException(e);
            throw e;
        }
    }

    String createLockId(String file)
    {
        return connector.getName() + "-" + endpoint.getEndpointURI().getPath() + "-" + file;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        this.lockFactory = getEndpoint().getMuleContext().getLockFactory();
        boolean synchronousProcessing = false;
        if (getFlowConstruct() instanceof Flow)
        {
            synchronousProcessing = ((Flow)getFlowConstruct()).getProcessingStrategy() instanceof SynchronousProcessingStrategy;
        }
        this.poolOnPrimaryInstanceOnly = Boolean.valueOf(System.getProperty("mule.transport.sftp.singlepollinstance","false")) || !synchronousProcessing;
    }

    @Override
    protected boolean pollOnPrimaryInstanceOnly()
    {
        return poolOnPrimaryInstanceOnly;
    }

    protected void routeFile(final String path) throws Exception
    {
        final ValueHolder<InputStream> inputStreamReference = new ValueHolder<>();
        ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();

        try
        {
            executionTemplate.execute(new ExecutionCallback<MuleEvent>()
            {
                @Override
                public MuleEvent process() throws Exception
                {
                    // A bit tricky initialization of the notifier in this case since we don't
                    // have access to the message yet...
                    SftpNotifier notifier = new SftpNotifier((SftpConnector) connector, createNullMuleMessage(),
                                                             endpoint, flowConstruct.getName());

                    InputStream inputStream = sftpRRUtil.retrieveFile(path, notifier);
                    inputStreamReference.set(inputStream);

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Routing file: " + path);
                    }

                    MuleMessage message = createMuleMessage(inputStream);

                    message.setProperty(SftpConnector.PROPERTY_FILENAME, path, PropertyScope.INBOUND);
                    message.setProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME, path, PropertyScope.INBOUND);

                    // Now we have access to the message, update the notifier with the message
                    notifier.setMessage(message);
                    routeMessage(message);

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Routed file: " + path);
                    }
                    return null;
                }
            });

            SftpStream sftpStream = getSftpStream(inputStreamReference);
            if (sftpStream != null)
            {
                sftpStream.performPostProcessingOnClose(true);
            }
        }
        catch (Exception e)
        {
            SftpStream sftpStream = getSftpStream(inputStreamReference);
            if (sftpStream != null)
            {
                sftpStream.setErrorOccurred();
            }
            throw e;
        }
        finally
        {
            SftpStream sftpStream = getSftpStream(inputStreamReference);
            if (sftpStream != null)
            {
                if (sftpStream.isClosed())
                {
                    sftpStream.postProcess();
                }
            }
        }
    }

    private SftpStream getSftpStream(ValueHolder<InputStream> inputStreamReference)
    {
        InputStream inputStream = inputStreamReference.get();
        if (inputStream instanceof SftpStream)
        {
            return (SftpStream) inputStream;
        }

        return null;
    }

    /**
     * SFTP-35
     */
    @Override 
    protected MuleMessage handleUnacceptedFilter(MuleMessage message) {
        logger.debug("the filter said no, now trying to close the payload stream");
        try {
            final SftpInputStream payload = (SftpInputStream) message.getPayload();
            payload.close();
        }
        catch (Exception e) {
            logger.debug("unable to close payload stream", e);
        }
        return super.handleUnacceptedFilter(message);
    }



    public void doConnect() throws Exception
    {
        if (connecting.compareAndSet(false, true) && !isConnected())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Connecting: " + this);
            }
            retryTemplate.execute(new RetryCallback()
            {
                @Override
                public void doWork(RetryContext context) throws Exception
                {
                    try
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Trying to connect/reconnect to SFTP server " + endpoint.getEndpointURI());
                        }
                        sftpRRUtil.getAvailableFiles(false);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Successfully connected/reconnected to SFTP server " + endpoint.getEndpointURI());
                        }
                        connected.set(true);
                        connecting.set(false);
                    }
                    catch (Exception e)
                    {
                        connected.set(false);
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Unable to connect/reconnect to SFTP server " + endpoint.getEndpointURI());
                        }
                        throw new Exception("Fail to connect", e);
                    }
                }

                @Override
                public String getWorkDescription()
                {
                    return "Trying to reconnect to SFTP server " + endpoint.getEndpointURI();
                }

                @Override
                public Connector getWorkOwner()
                {
                    return getEndpoint().getConnector();
                }
            }, getConnector().getMuleContext().getWorkManager());
        }

    }

    public void doDisconnect() throws Exception
    {
        // no op
    }

    protected void doDispose()
    {
        // no op
    }
}
